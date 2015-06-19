package thu.ic.collavoid.storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import thu.ic.collavoid.commons.planners.AgentState;
import thu.ic.collavoid.commons.planners.Neighbor;
import thu.ic.collavoid.commons.rmqmsg.*;
import thu.ic.collavoid.commons.storm.Constant_storm;
import com.esotericsoftware.kryo.Kryo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentStateBolt extends BaseRichBolt {
    Logger logger= LoggerFactory.getLogger(AgentStateBolt.class);
    OutputCollector collector;
    Kryo kryo;
    Map<String, Double> truncTimeMap=new ConcurrentHashMap<>();
    Map<String, PoseShareMsg_> poseShareMsgMap = new ConcurrentHashMap<>();
    Map<String, Odometry_> odometryMap = new ConcurrentHashMap<>();
    Map<String, PoseShareMsg_> neighborShareMsgMap = new ConcurrentHashMap<>();
    Map<String, PointCloud2_> scanMap = new ConcurrentHashMap<>();
    Map<String, PoseArray_> poseArrayMap = new ConcurrentHashMap<>();
    long arrTime;
    long finishTime;
    ScheduledExecutorService executor;
    
    //test delay
//    RabbitMQSender sender;
//    Map<String,Object> delayMsgMap=new HashMap<>();

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        collector = outputCollector;
        kryo = Methods_RMQ.getKryo();

        executor= Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    List<String> timeOutRobots = new ArrayList<String>();
                    for (Map.Entry<String, Odometry_> e : odometryMap.entrySet()) {
                        if (now - e.getValue().getHeader().getStamp() > Constant_storm.Misc.ODOMETRY_TIMEOUT * 1000) {
                            timeOutRobots.add(e.getKey());
                        }
                    }
                    clearTimeOutRobots(timeOutRobots);
                }catch (Exception e){
                    ;
                }

            }
        }, 1, Constant_storm.Misc.CHECK_ODOMETRY_TIMEOUT_INTERVAL, TimeUnit.SECONDS);
    }

    private void clearTimeOutRobots(List<String> timeOutRobots){
        for (String id:timeOutRobots) {
            logger.warn("Robot {} timeout, will clear cached data!",id);
            truncTimeMap.remove(id);
            poseShareMsgMap.remove(id);
            odometryMap.remove(id);
            neighborShareMsgMap.remove(id);
            scanMap.remove(id);
            poseArrayMap.remove(id);
        }
        List<Object> tuple=new ArrayList<>();
        tuple.add(timeOutRobots);
        collector.emit(Constant_storm.Streams.ROBOT_TIMEOUT_STREAM,tuple);
    }

    @Override
    public void execute(Tuple tuple){
        arrTime=System.currentTimeMillis();
        if (tuple.getSourceStreamId().equals(Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM)) {
            emitAgentState(tuple);
        } else if (tuple.getSourceStreamId().equals(Constant_storm.Streams.PUBLISHME_STREAM)) {
            emitPoseShare(tuple);
        } else if (tuple.getSourceComponent().equals(Constant_storm.Components.ODOMETRY_SPOUT_COMPONENT)) {
            cacheOdometry(tuple);
//            logger.info("received odom");
        } else if (tuple.getSourceComponent().equals(Constant_storm.Components.POSE_SHARE_COMPONENT)) {
            cacheNeighborPoseShareMsg(tuple);
//            logger.info("received pose share");
        } else if (tuple.getSourceComponent().equals(Constant_storm.Components.SCAN_COMPONENT)) {
            cacheScan(tuple);
//            logger.info("received scan");
        } else if (tuple.getSourceComponent().equals(Constant_storm.Components.POSE_ARRAY_COMPONENT)) {
            cachePoseArray(tuple);
//            logger.info("received pose array");
        } else if (tuple.getSourceComponent().equals(Constant_storm.Components.GLOBAL_PLANNER_COMPONENT)) {
            cachePoseShareMsg(tuple);
            cacheTruncTime(tuple);
        }

        collector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM, new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.AGENT_STATE_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.EMIT_TIME_FIELD

        ));
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.PUBLISHME_PUB_STREAM, new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.POSE_SHARE_FIELD
        ));
        // back to dispatcher
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.PUBLISHME_STREAM, new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.EMIT_TIME_FIELD

        ));
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.ACK_STREAM, new Fields(
                Constant_storm.FIELDS.SESSION_ID_FIELD,
                Constant_storm.FIELDS.AGENT_IDX_FIELD,
                Constant_storm.FIELDS.SEQUENCE_FIELD

        ));
        outputFieldsDeclarer.declareStream(Constant_storm.Streams.ROBOT_TIMEOUT_STREAM,new Fields(
            Constant_storm.FIELDS.TIMEOUT_ROBOT_LIST
        ));
    }

    private void emitPoseShare(Tuple tuple) {
        PoseShareMsg_ msg;
        String id = tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD);
        String times=tuple.getStringByField(Constant_storm.FIELDS.EMIT_TIME_FIELD);
        times=times+arrTime+",";
        if (poseShareReady(id)) {
            msg = poseShareMsgMap.get(id);
            Odometry_ odometry_ = odometryMap.get(msg.getId());
            msg.getHeader().setStamp(odometry_.getHeader().getStamp());
            msg.setPose(odometry_.getPose().copy());
            msg.setTwist(odometry_.getTwist().copy());
            if (poseArrayMap.containsKey(msg.getId())){
                if(msg.getFootprint_original().size()==0){
                    throw new RuntimeException("=========original foot print zero");
                }
                msg.setFootPrint_Minkowski(
                        Methods.getMinkowskiFootprint(msg.getFootprint_original(), poseArrayMap.get(msg.getId())));
            }
            collector.emit(Constant_storm.Streams.PUBLISHME_PUB_STREAM, new Values(
                    tuple.getValue(0),
                    tuple.getValue(1),
                    Methods_RMQ.serialize(kryo, msg)
            ));
        }
        finishTime=System.currentTimeMillis();
        collector.emit(Constant_storm.Streams.PUBLISHME_STREAM, new Values(
                tuple.getValue(0),
                tuple.getValue(1),
                tuple.getValueByField(Constant_storm.FIELDS.AGENT_IDX_FIELD),
                times+finishTime+","
        ));
    }

    private boolean poseShareReady(String id) {
        if (odometryMap.containsKey(id)&&poseShareMsgMap.containsKey(id))
            return true;
        return false;
    }

    private void emitAgentState(Tuple tuple) {
        String sensorID = tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD);

        String times=tuple.getStringByField(Constant_storm.FIELDS.EMIT_TIME_FIELD);
        times=times+arrTime+",";

        AgentState agentState=null;

        if (AgentStateReady(sensorID)){

            agentState=new AgentState(sensorID);
            //get odometry
            agentState.odometry_=odometryMap.get(sensorID);

            //get neighbors
            agentState.neighbors=getNeighbors(sensorID);

            //get obstacles
            if (scanMap.containsKey(sensorID)) {
                agentState.obstacles=Methods.getObstacles(agentState.neighbors,
                        poseShareMsgMap.get(sensorID).getRadius(),
                        scanMap.get(sensorID));
            }

            //get minkowski footprint
            if (poseArrayMap.containsKey(sensorID)) {
                agentState.minkowskiFootprint=Methods.getMinkowskiFootprint(
                        poseShareMsgMap.get(sensorID).getFootprint_original(),
                        poseArrayMap.get(sensorID));
            }
        }
        finishTime=System.currentTimeMillis();
        collector.emit(Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM, new Values(
                tuple.getValue(0),
                tuple.getValue(1),
                agentState,
                tuple.getValueByField(Constant_storm.FIELDS.AGENT_IDX_FIELD),
                times+finishTime+","
        ));
    }

    private boolean AgentStateReady(String sensorID) {
        if (odometryMap.containsKey(sensorID) &&
                poseShareMsgMap.containsKey(sensorID))
            return true;
        return false;
    }

    private List<Neighbor> getNeighbors(String id) {
        List<Neighbor> neighbors = new ArrayList<>();
        Odometry_ odom=odometryMap.get(id);
        double trunctime=truncTimeMap.get(id);
        double radius=poseShareMsgMap.get(id).getRadius();

        for (Map.Entry<String, PoseShareMsg_> e : neighborShareMsgMap.entrySet()) {
            if (!e.getKey().equals(id)) {
                if (Methods.isCloseNeighbor(odom,radius, e.getValue(),trunctime)) {
                    neighbors.add(Methods.extractNeighbor(e.getValue()));
//                    logger.info("neighbor delay {}",agent.last_seen_-e.getValue().getHeader().getStamp());
                }
            }
        }
        return neighbors;
    }

    private void cacheTruncTime(Tuple tuple) {
        truncTimeMap.put(tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD),
                tuple.getDoubleByField(Constant_storm.FIELDS.TRUNC_TIME_FIELD));
    }

    private void cachePoseShareMsg(Tuple tuple) {
        String sensorID=tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD);
        PoseShareMsg_ msg=(PoseShareMsg_) tuple.getValueByField(Constant_storm.FIELDS.POSE_SHARE_FIELD);
        poseShareMsgMap.put(sensorID,msg);
        collector.emit(Constant_storm.Streams.ACK_STREAM,new Values(
                sensorID,
                tuple.getValueByField(Constant_storm.FIELDS.AGENT_IDX_FIELD),
                msg.getHeader().getSeq()
                ));
//        logger.info("received new pose share config {}",tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD));

    }

    private void cachePoseArray(Tuple tuple) {
        poseArrayMap.put(tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD),
                (PoseArray_) tuple.getValueByField(Constant_storm.FIELDS.POSE_ARRAY_FIELD));
    }

    private void cacheScan(Tuple tuple) {
        scanMap.put(tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD),
                (PointCloud2_) tuple.getValueByField(Constant_storm.FIELDS.SCAN_FIELD));
    }

    private void cacheNeighborPoseShareMsg(Tuple tuple) {
        neighborShareMsgMap.put(tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD),
                (PoseShareMsg_) tuple.getValueByField(Constant_storm.FIELDS.POSE_SHARE_FIELD));
    }

    private void cacheOdometry(Tuple tuple) {
        Odometry_ odometry_ = (Odometry_) tuple.getValueByField(Constant_storm.FIELDS.ODOMETRY_FIELD);
        double vx = odometry_.getTwist().getLinear().getX();
        double vy = odometry_.getTwist().getLinear().getY();
        double vxTransformed = Math.sqrt(vx * vx + vy * vy);
        odometry_.getTwist().getLinear().setX(vxTransformed);
        odometry_.getTwist().getLinear().setY(0.0);
        odometryMap.put(tuple.getStringByField(Constant_storm.FIELDS.SESSION_ID_FIELD), odometry_);
    }
}
