package thu.ic.collavoid.storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import thu.ic.collavoid.commons.planners.AgentCtlPubState;
import thu.ic.collavoid.commons.rabbitmq.Message;
import thu.ic.collavoid.commons.storm.Constant_storm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DispatcherBolt extends BaseRichBolt {
    Logger logger = LoggerFactory.getLogger(DispatcherBolt.class);
    OutputCollector collector;
    Map<String, AgentCtlPubState> timeMap = new ConcurrentHashMap<>();
    Map<String, Integer> cntMap = new ConcurrentHashMap<>();
    Map<String,Integer> idxMap=new ConcurrentHashMap<>();
    long arrTime;

    //test delay
//    RabbitMQSender sender;
    Map<Integer,String> clDelayMsgMap=new HashMap<>();
    Map<Integer,String> slDelayMsgMap=new HashMap<>();
    
    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        collector = outputCollector;
//        sender=new RabbitMQSender(Constants.localRMQUrl,Constants.exchangeName);
//        try {
//            sender.open(Constants.exchangeType);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void execute(Tuple tuple) {
        arrTime=System.currentTimeMillis();
        if (tuple.getSourceComponent().equals(Constant_storm.Components.TIMER_SPOUT_COMPONENT)) {
            checkNewEmit(tuple);
        } else if (tuple.getSourceComponent().equals(Constant_storm.Components.GLOBAL_PLANNER_COMPONENT)) {
            cacheTimes(tuple);
        } else if (tuple.getSourceStreamId().equals(Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM)) {
            switchCtlState(tuple);
        } else if (tuple.getSourceStreamId().equals(Constant_storm.Streams.PUBLISHME_STREAM)) {
            switchPubState(tuple);
        }else if (tuple.getSourceStreamId().equals(Constant_storm.Streams.ROBOT_TIMEOUT_STREAM)){
            clearTimeoutData((List<String>)tuple.getValueByField(Constant_storm.FIELDS.TIMEOUT_ROBOT_LIST));
        }
        collector.ack(tuple);
    }

    private void clearTimeoutData(List<String> ids){
        for (String id:ids){
            timeMap.remove(id);
            cntMap.remove(id);
            clDelayMsgMap.remove(idxMap.get(id));
            slDelayMsgMap.remove(idxMap.get(id));
            idxMap.remove(id);
        }
    }

    private void switchPubState(Tuple tuple) {
        String id=tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD);
        String times=tuple.getStringByField(Constant_storm.FIELDS.EMIT_TIME_FIELD);
        times=times+arrTime+",";
        if (timeMap.containsKey(id)&&timeMap.get(id).publishing){
            timeMap.get(id).publishing = false;

            slDelayMsgMap.put(idxMap.get(id),times);

        }
    }

    private void switchCtlState(Tuple tuple) {
        String id=tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD);
        String times=tuple.getStringByField(Constant_storm.FIELDS.EMIT_TIME_FIELD);
        int taskid=tuple.getIntegerByField(Constant_storm.FIELDS.TASK_ID_FIELD);
        times=times+arrTime+","+taskid;
        if ((Boolean)tuple.getValueByField(Constant_storm.FIELDS.AGENT_STATE_FIELD)){
            timeMap.remove(id);
            idxMap.remove(id);
            logger.info("Robot {} reached goal!",id);
        }
        if (timeMap.containsKey(id)&&timeMap.get(id).controlling){
            timeMap.get(tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD)).controlling = false;
            clDelayMsgMap.put(idxMap.get(id),times);
        }
    }

    private void cacheTimes(Tuple tuple) {
        String id=tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD);
        // only used for the first time
        if (!cntMap.containsKey(id))
            cntMap.put(id, new Integer(0));
        AgentCtlPubState state=(AgentCtlPubState) tuple.getValueByField(Constant_storm.FIELDS.CTL_PUB_TIME_FIELD);
        timeMap.put(id,state);
        // for grouping
        idxMap.put(id,(Integer)tuple.getValueByField(Constant_storm.FIELDS.AGENT_IDX_FIELD));
        collector.emit(Constant_storm.Streams.ACK_STREAM,new Values(
                id,
                tuple.getValueByField(Constant_storm.FIELDS.AGENT_IDX_FIELD),
                state.seq
                ));
//        logger.info("received new time config {}", tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD));
    }

    private void checkNewEmit(Tuple tuple) {
        long now;

        for (Map.Entry<String, AgentCtlPubState> e : timeMap.entrySet()) {
            AgentCtlPubState state = e.getValue();
            now = System.currentTimeMillis();
            Integer idx=idxMap.get(e.getKey());
            if (!state.publishing && (now - state.lastTimeMePublished > state.publishMePeriod * 1000 - 5)) {
                state.lastTimeMePublished = now;
                state.publishing=true;
                String emitTime="sl,"+System.currentTimeMillis()+',';
                collector.emit(Constant_storm.Streams.PUBLISHME_STREAM, new Values(
                        now,
                        e.getKey(),
                        idx,
                        emitTime

                ));

                if (slDelayMsgMap.containsKey(idx)){
                    sendMsg(idx,slDelayMsgMap.remove(idx),false);
                }
            }

            if (cntMap.get(e.getKey()) < 50) {
                // establish stable state
                cntMap.put(e.getKey(), new Integer(cntMap.get(e.getKey()) + 1));
            }else {
                if (!state.controlling && (now - state.lastTimeControlled > state.controlPeriod * 1000 - 5)) {
                    state.lastTimeControlled = now;
                    state.controlling=true;
//                    logger.info("agent {} cmd {}",agent.id,agent.cmd_vel.toString());
                    String emitTime="cl,"+System.currentTimeMillis()+',';
                    collector.emit(Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM, new Values(
                            now,
                            e.getKey(),
                            idxMap.get(e.getKey()),
                            emitTime
                    ));

                    if (clDelayMsgMap.containsKey(idx)){
                        sendMsg(idx,clDelayMsgMap.remove(idx),true);
                    }
                }
            }
        }

    }

    private void sendMsg(Integer idx,String delays,boolean ctl){
        Map<String,Object> prop=new HashMap<>();
        if(!ctl) {
            prop.put(idx.toString(), delays + System.currentTimeMillis());
        }else {
            prop.put(idx.toString(),insertTime(delays,System.currentTimeMillis()));
        }

        Message message=new Message(("").getBytes(),prop);
//        try {
//            sender.send(message,Constants.routingKey);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private String insertTime(String delays,long time){
        int pos=delays.lastIndexOf(",");
        return delays.substring(0,pos+1)+time+","+delays.substring(pos+1,delays.length());
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM, new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.EMIT_TIME_FIELD

        ));
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.PUBLISHME_STREAM, new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.EMIT_TIME_FIELD

        ));
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.ACK_STREAM,new Fields(
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.SEQUENCE_FIELD

        ));
    }


}
