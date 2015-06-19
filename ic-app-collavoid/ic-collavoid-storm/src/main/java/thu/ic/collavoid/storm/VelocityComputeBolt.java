package thu.ic.collavoid.storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import thu.ic.collavoid.commons.planners.Agent;
import thu.ic.collavoid.commons.planners.AgentState;
import thu.ic.collavoid.commons.rmqmsg.Methods_RMQ;
import thu.ic.collavoid.commons.storm.Constant_storm;
import com.esotericsoftware.kryo.Kryo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityComputeBolt extends BaseRichBolt {
    Logger logger = LoggerFactory.getLogger(VelocityComputeBolt.class);
    Kryo kryo;
    OutputCollector collector;
    Map<String, Agent> agentMap = new ConcurrentHashMap<>();
    Map<String, String> moduleMap = new ConcurrentHashMap<>();
    long arrTime;
    long emitTime;
    int taskid;
    long msgId;

    //test delay
//    RabbitMQSender sender;
//    Map<String,Object> delayMsgMap=new HashMap<>();

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        collector = outputCollector;
        kryo = Methods_RMQ.getKryo();
        taskid = topologyContext.getThisTaskId();
//        sender=new RabbitMQSender(Constants.localRMQUrl,Constants.exchangeName);
//        try {
//            sender.open(Constants.exchangeType);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void execute(Tuple tuple) {
        arrTime = System.currentTimeMillis();
        if (tuple.getSourceStreamId().equals(Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM)) {
            emitNewCmd(tuple);
        } else if (tuple.getSourceComponent().equals(Constant_storm.Components.GLOBAL_PLANNER_COMPONENT)) {
            cacheAgent(tuple);
        }else if (tuple.getSourceStreamId().equals(Constant_storm.Streams.ROBOT_TIMEOUT_STREAM)){
            clearTimeoutData((List<String>)tuple.getValueByField(Constant_storm.FIELDS.TIMEOUT_ROBOT_LIST));
        }
        collector.ack(tuple);
    }

    private void clearTimeoutData(List<String> ids){
        for (String id:ids){
            agentMap.remove(id);
            moduleMap.remove(id);
        }
    }

    private void cacheAgent(Tuple tuple) {
        Agent agent = (Agent) tuple.getValueByField(Constant_storm.FIELDS.AGENT_FIELD);
//        logger.info("received new config {}",agent.id);
        agentMap.put(agent.id, agent);
        moduleMap.put(agent.id, tuple.getStringByField(Constant_storm.FIELDS.MODULE_ID_FIELD));
        collector.emit(Constant_storm.Streams.ACK_STREAM, new Values(
                agent.id,
                tuple.getValueByField(Constant_storm.FIELDS.AGENT_IDX_FIELD),
                agent.getSeq()
        ));
    }

    private void emitNewCmd(Tuple tuple) {
        boolean goalReached = false;
        Object input = tuple.getValueByField(Constant_storm.FIELDS.AGENT_STATE_FIELD);
        String times = tuple.getStringByField(Constant_storm.FIELDS.EMIT_TIME_FIELD);
        times = times + arrTime + ",";
        if (input != null) {
            AgentState agentState = (AgentState) input;
            Agent agent = agentMap.get(agentState.id);
            int flag = updateAgentState(agentState, agent);
            if (flag == 0) {
                Methods.getCommand(agent);
                String moduleId=moduleMap.get(agentState.id);
                if (agent.cmd_vel.isGoalReached()) {
                    agentMap.remove(agentState.id);
                    moduleMap.remove(agentState.id);
                    goalReached = true;
                }
                collector.emit(Constant_storm.Streams.VELOCITY_COMMAND_STREAM, new Values(
                        tuple.getValue(0),
                        moduleId,
                        tuple.getValue(1),
                        msgId++,
                        Methods_RMQ.serialize(kryo, agent.cmd_vel)));
            } else if (flag < 0) {
                //may need to change!!!!!-------------------------------old agent
//                agentMap.remove(agentState.id);
//                goalReached = true;
//                logger.info("removed agent {}",agentState.id);
            } else {
//                logger.info("new calculation pending {}",agentState.id);
            }
        }
        emitTime = System.currentTimeMillis();
//      ack dispatcher
        collector.emit(Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM, new Values(
                tuple.getValue(0),
                tuple.getValue(1),
                goalReached,
                times + emitTime + ",",
                tuple.getValueByField(Constant_storm.FIELDS.AGENT_IDX_FIELD),
                taskid
        ));
    }

    private int updateAgentState(AgentState agentState, Agent agent) {
        // agent is not received from global planner or odometry is old
        if (null == agent || agent.getSeq() > agentState.odometry_.getHeader().getSeq()) {
            return 1;
        } else if (agent.getSeq() == agentState.odometry_.getHeader().getSeq()) {
            agent.setBase_odom_(agentState.odometry_);
            agent.setAgentNeighbors(agentState.neighbors);
            agent.setObstacles_from_laser_(agentState.obstacles);
            agent.setFootprint_minkowski(agentState.minkowskiFootprint);
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.VELOCITY_COMMAND_STREAM, new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.MODULE_ID_FIELD,
                Constant_storm.FIELDS.SESSION_ID_FIELD,
//                TODO: deprecate message id field
                Constant_storm.FIELDS.MESSAGE_ID_FIELD,
                Constant_storm.FIELDS.VELOCITY_COMMAND_FIELD
        ));
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM, new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.AGENT_STATE_FIELD,
                Constant_storm.FIELDS.EMIT_TIME_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.TASK_ID_FIELD

        ));
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.ACK_STREAM, new Fields(
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.SEQUENCE_FIELD

        ));
    }
}
