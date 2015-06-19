package thu.ic.collavoid.storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import thu.ic.collavoid.commons.planners.Agent;
import thu.ic.collavoid.commons.planners.AgentCtlPubState;
import thu.ic.collavoid.commons.planners.Methods_Planners;
import thu.ic.collavoid.commons.rmqmsg.BaseConfig_;
import thu.ic.collavoid.commons.rmqmsg.PoseShareMsg_;
import thu.ic.collavoid.commons.storm.Constant_storm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalPlannerBolt extends BaseRichBolt {
    private Logger logger = LoggerFactory.getLogger(GlobalPlannerBolt.class);
    private OutputCollector outputCollector;
    private Map<String,Agent> agentMap=new ConcurrentHashMap<>();
    private Map<String,PoseShareMsgAndTrunc> poseShareMsgMap=new ConcurrentHashMap<>();
    private Map<String,AgentCtlPubState> agentCtlPubStateMap=new ConcurrentHashMap<>();
    private Map<String,Integer> idxMap=new ConcurrentHashMap<>();
    private Map<String,String> moduleMap=new ConcurrentHashMap<>();

    private class PoseShareMsgAndTrunc{
        public PoseShareMsg_ msg;
        public double truncTime;

        public PoseShareMsgAndTrunc(){

        }
    }

    @Override
    public void execute(Tuple input) {
        if (input.getSourceComponent().equals(Constant_storm.Components.COMMAND_SPOUT_COMPONENT)){
            logger.info("received new config");
            cacheConfigAndEmit(input);
        }else if (input.getSourceComponent().equals(Constant_storm.Components.TIMER_SPOUT_COMPONENT)){
            reEmit();
        }else {
            confirmConfig(input);
        }
        outputCollector.ack(input);
    }

    private void cacheConfigAndEmit(Tuple input){
        BaseConfig_ baseConfig_ = (BaseConfig_) input.getValueByField(Constant_storm.FIELDS.BASE_CONFIG_FIELD);
        Agent agent = new Agent(input.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD));
        agent.setPlan(Methods_Planners.makePlan(baseConfig_.getStart(), baseConfig_.getGoal()));
        agent.setSeq(baseConfig_.getSeq());
        agent.lastTimeControlled = input.getLongByField(Constant_storm.FIELDS.TIME_FIELD);
        agent.lastTimeMePublished = input.getLongByField(Constant_storm.FIELDS.TIME_FIELD);
        agentMap.put(agent.id,agent);
        Integer idx=(Integer)input.getValueByField(Constant_storm.FIELDS.AGENT_IDX_FIELD);
        idxMap.put(agent.id,idx);
        moduleMap.put(agent.id,input.getStringByField(Constant_storm.FIELDS.MODULE_ID_FIELD));
        outputCollector.emit(Constant_storm.Streams.AGENT_STREAM,
                new Values(input.getValue(0), input.getValue(1),input.getValue(2), idx,agent));

        PoseShareMsgAndTrunc poseShareMsgAndTrunc=new PoseShareMsgAndTrunc();
        poseShareMsgAndTrunc.msg= Methods.extractPoseShareMsgFromAgent(agent);
        poseShareMsgAndTrunc.truncTime=agent.truncTime;
        poseShareMsgMap.put(agent.id,poseShareMsgAndTrunc);
        outputCollector.emit(Constant_storm.Streams.POSE_SHARE_MSG_STREAM,new Values(
                input.getValue(0),
                input.getValue(2),
                poseShareMsgAndTrunc.msg,
                idx,
                agent.truncTime

        ));

        AgentCtlPubState agentCtlPubState=new AgentCtlPubState();
        agentCtlPubState.controlPeriod=agent.getControlPeriod();
        agentCtlPubState.publishMePeriod=agent.getPublishMePeriod();
        agentCtlPubState.lastTimeControlled=agent.lastTimeControlled;
        agentCtlPubState.lastTimeMePublished=agent.getLastTimeMePublished();
        agentCtlPubState.seq=baseConfig_.getSeq();
        agentCtlPubStateMap.put(agent.id,agentCtlPubState);
        outputCollector.emit(Constant_storm.Streams.CTL_PUB_TIME_STREAM,new Values(
                input.getValue(0),
                input.getValue(2),
                idx,
                agentCtlPubState
        ));

    }

    private void reEmit(){
        for (Map.Entry<String,Agent> e:agentMap.entrySet()){
            outputCollector.emit(Constant_storm.Streams.AGENT_STREAM,
                    new Values(System.currentTimeMillis(),
                            moduleMap.get(e.getKey()),
                            e.getKey(),
                            idxMap.get(e.getKey()),
                            e.getValue()
                            ));
        }
        for (Map.Entry<String,PoseShareMsgAndTrunc> e:poseShareMsgMap.entrySet()){
            outputCollector.emit(Constant_storm.Streams.POSE_SHARE_MSG_STREAM,new Values(
                    System.currentTimeMillis(),
                    e.getKey(),
                    e.getValue().msg,
                    idxMap.get(e.getKey()),
                    e.getValue().truncTime

            ));
        }
        for (Map.Entry<String,AgentCtlPubState> e:agentCtlPubStateMap.entrySet()){
            outputCollector.emit(Constant_storm.Streams.CTL_PUB_TIME_STREAM,new Values(
                    System.currentTimeMillis(),
                    e.getKey(),
                    idxMap.get(e.getKey()),
                    e.getValue()
            ));
        }

    }

    private void confirmConfig(Tuple input){
        String sensorID=input.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD);
        long seq=input.getLongByField(Constant_storm.FIELDS.SEQUENCE_FIELD);
        String compID=input.getSourceComponent();
        if (compID.equals(Constant_storm.Components.AGENT_STATE_COMPONENT)){
            if (poseShareMsgMap.containsKey(sensorID)&&poseShareMsgMap.get(sensorID).msg.getHeader().getSeq()==seq){
                poseShareMsgMap.remove(sensorID);
            }
        }else if (compID.equals(Constant_storm.Components.DISPATCHER_COMPONENT)){
            if (agentCtlPubStateMap.containsKey(sensorID)&&agentCtlPubStateMap.get(sensorID).seq==seq){
                agentCtlPubStateMap.remove(sensorID);
            }
        }else if (compID.equals(Constant_storm.Components.VELOCITY_COMPUTE_COMPONENT)){
            if (agentMap.containsKey(sensorID)&&agentMap.get(sensorID).getSeq()==seq){
                agentMap.remove(sensorID);
                moduleMap.remove(sensorID);
            }
        }
        if (!poseShareMsgMap.containsKey(sensorID)
                &&!agentCtlPubStateMap.containsKey(sensorID)
                &&!agentMap.containsKey(sensorID)){
            idxMap.remove(sensorID);
        }
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        outputCollector = collector;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(Constant_storm.Streams.AGENT_STREAM,new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.MODULE_ID_FIELD,
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.AGENT_FIELD
        ));
        declarer.declareStream(Constant_storm.Streams.POSE_SHARE_MSG_STREAM,new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.POSE_SHARE_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.TRUNC_TIME_FIELD

        ));
        declarer.declareStream(Constant_storm.Streams.CTL_PUB_TIME_STREAM,new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.CTL_PUB_TIME_FIELD

        ));
    }

}
