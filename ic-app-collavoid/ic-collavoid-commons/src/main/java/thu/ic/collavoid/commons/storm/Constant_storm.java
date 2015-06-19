package thu.ic.collavoid.commons.storm;

import backtype.storm.tuple.Fields;
import thu.ic.collavoid.commons.rmqmsg.Constant_msg;
import thu.ic.collavoid.commons.rmqmsg.IotRMQContext;

import java.util.HashMap;
import java.util.Map;

public abstract class Constant_storm {
    public abstract static class FIELDS {
        //general
        public static final String TIME_FIELD="time";
//      one session correspond to one sensor
        public static final String SESSION_ID_FIELD ="sessionId";
        public static final String MESSAGE_ID_FIELD = "messageId";
        public static final String MODULE_ID_FIELD = "moduleId";
        //global planner
        public static final String START_GOAL_FIELD="startGoal";
        public static final String BASE_CONFIG_FIELD="baseConfig";//in streaming
        public static final String PLAN_FIELD="plan";
        //local planner
        public static final String PREFERRED_VELOCITY_FIELD="preferredVelocity";
        public static final String VELOCITY_COMMAND_FIELD="velocityCommand";
        public static final String RESET_FIELD="reset";
        //odometry
        public static final String ODOMETRY_FIELD="odometry";
        //scan get obstacles
        public static final String SCAN_FIELD="scan";
        public static final String ALL_AGENTS_FIELD ="agents";
        public static final String OBSTACLE_FIELD="obstacles";
        //pose share spout
        public static final String POSE_SHARE_FIELD="poseShare";        
        // get minkowski footprint
        public static final String FOOTPRINT_OWN_FIELD="footprintOwn";
        public static final String POSE_ARRAY_FIELD = "pose_array";
        public static final String FOOTPRINT_MINKOWSK_FIELD="minkowskiFootprint";
        // compute vo lines
        public static final String AGENT_FIELD="agent";
        public static final String ACC_LINES_FIELD ="accLines";// to joint them need to separate each type
        public static final String NH_LINES_FIELD ="nhLines";
        public static final String OBSTACLE_LINES_FIELD ="obstacleLines";// for orca
        public static final String AGENT_VO_FIELD="agentVO";
        public static final String OBSTACLE_VO_FIELD="obstacleVO";
        public static final String SEQUENCE_FIELD="sequence";

        public static final String ORCA_LINES_FIELD = "orcaLines";
        public static final String VOS_FIELD = "vos";
        //agent state
        public static final String NEIGHBORS_FIELD = "neighbors";
        // timer
        public static final String TIMER_TICK_FIELD = "timerTick";
        //control command
        public static final String COMMAND_MAP_FILED = "commandMap";
        public static final String COMMAND_FILED = "command";

        //test time delay
        public static final String EMIT_TIME_FIELD = "emitTime";
        public static final String TASK_ID_FIELD="taskid";

        public static final String TIMEOUT_ROBOT_LIST="timeoutRobots";

        public static final Fields JOIN_FIELDS=new Fields(
                AGENT_FIELD,
                ACC_LINES_FIELD,
                NH_LINES_FIELD,
                OBSTACLE_LINES_FIELD,
                AGENT_VO_FIELD,
                OBSTACLE_VO_FIELD,
                EMIT_TIME_FIELD
        );

        public static final Fields JOIN_FIELDS_STATELESS=new Fields(
                AGENT_FIELD,
                ACC_LINES_FIELD,
                NH_LINES_FIELD,
                OBSTACLE_LINES_FIELD,
                AGENT_VO_FIELD,
                OBSTACLE_VO_FIELD
        );

        // simple topology
        public static final String FOOTPRINT_RADIUS_FIELD="footprintRadius";
        public static final String AGENT_STATE_FIELD="agentState";
        public static final String CTL_PUB_TIME_FIELD="ctlPubTime";
        public static final String TRUNC_TIME_FIELD="truncTime";
        public static final String AGENT_IDX_FIELD="agentIndex";
    }

    public abstract class Streams{
        //local planner
        public static final String PREFERRED_VELOCITY_STREAM="preferredVelocity";
        public static final String VELOCITY_COMMAND_STREAM="velocityCommand";
        public static final String FOOTPRINT_OWN_STREAM="footprintStream";
        public static final String PUBLISHME_STREAM="publishMe";
        public static final String CALCULATE_VELOCITY_CMD_STREAM ="calculateVelocity";
        public static final String PUBLISH_ME_TIMER_STREAM="publishMeTimer";
        public static final String CONTROLLER_TIMER_STREAM="controllerTimer";
        public static final String NEIGHBORS_STREAM="neighbors";
        public static final String RESET_STREAM="reset";
        public static final String COMMAND_STREAM = "command";
        public static final String CONFIG_STREAM = "config";
        public static final String TIMEOUT_STREAM = "timeout";
        public static final String ROBOT_TIMEOUT_STREAM="robotTimeout";

        public static final String ODOMETRY_NULL_STREAM="odometryNull";
        public static final String PUBLISHME_PUB_STREAM="mePublisher";

        // simple topology
        public static final String CTL_PUB_TIME_STREAM="ctlPubTime";
        public static final String CTLPUB_TIMEER_STREAM="ctlPubTimer";
        public static final String POSE_SHARE_MSG_STREAM="poseShareMsg";
        public static final String AGENT_STREAM="agentStream";
        public static final String ACK_STREAM="ack";
    }

    public abstract class Components{
        public static final String GLOBAL_PLANNER_COMPONENT="globalPlanner";
        public static final String ODOMETRY_SPOUT_COMPONENT = "odometry_receiver";
        public static final String LOCAL_PLANNER_COMPONENT="localPlanner";
        public static final String AGENT_COMPONENT="agent";
        public static final String SCAN_COMPONENT="scan_receiver";
        public static final String GET_ALL_AGENTS_COMPONENT ="getAgents";
        public static final String POSE_SHARE_COMPONENT = "pose_share_out_receiver";
        public static final String POSE_ARRAY_COMPONENT ="pose_array_receiver";
        public static final String GET_OBSTACLES_COMPONENT="getObstacles";
        public static final String ODOMETRY_TRANSFORM_COMPONENT="odometryTransform";
        public static final String COMMAND_SPOUT_COMPONENT = "command_receiver";// in ic
        public static final String BASE_CONFIG_COMPONENT ="base_config_receiver";// in streaming
        public static final String VELOCITY_COMMAND_PUBLISHER_COMPONENT = "vel_cmd_sender";
        public static final String VELOCITY_COMPUTE_COMPONENT="velocityCompute";
        public static final String AGENT_STATE_COMPONENT="agentState";
        public static final String TIMER_SPOUT_COMPONENT ="timerSpout";//in streaming
        public static final String TIMER_COMPONENT ="timer";
        public static final String ACC_CONSTRAINTS_COMPONENT="accelerationConstraints";
        public static final String NH_CONSTRAINTS_COMPONENT="nhConstraints";
        public static final String VO_AGENT_COMPONENT="voAgent";
        public static final String VO_OBSTACLE_COMPONENT="voObstacle";
        public static final String VO_LINES_JOIN_COMPONENT="voLineJoin";
        public static final String VO_LINES_COMPUTE_COMPONENT = "voLinesCompute";
        public static final String GET_MINKOWSKI_FOOTPRINT_COMPONENT="getMinkowskiFootprint";
        public static final String POSE_SHARE_PUB_COMPONENT = "pose_share_in_sender";// iotcloud component
        public static final String GLOBAL_CONTROL_COMPONENT = "globalControl";

        public static final String CONTROLLER_COMPONENT = "Controller";

        //stateless topology
        public static final String ODOMETRY_COMPONENT="odometry";
        public static final String CHECK_AGENT_COMPONENT="checkAgent";
        public static final String DISPATCHER_COMPONENT="dispatcher";
        public static final String GET_NEIGHBORS_COMPONENT="getNeighbors";
        public static final String UPDATE_AGENT_COMPONENT="updateAgent";
    }
    public static class KeyToComponentMap{
        public final Map<String,String> map=new HashMap<String, String>();
        public KeyToComponentMap(){
            map.put(Constant_msg.KEY_SCAN, Components.SCAN_COMPONENT);
            map.put(Constant_msg.KEY_BASE_CONFIG,Components.BASE_CONFIG_COMPONENT);
            map.put(Constant_msg.KEY_POSE_SHARE,Components.POSE_SHARE_COMPONENT);
            map.put(Constant_msg.KEY_ODOMETRY,Components.ODOMETRY_SPOUT_COMPONENT);
            map.put(Constant_msg.KEY_POSE_ARRAY, Components.POSE_ARRAY_COMPONENT);
        }
    }

    public abstract class IC {
        public abstract class channels {
            public static final String ODOMETRY_CHANNEL = "odometry";
            public static final String SCAN_CHANNEL = "scan";
            public static final String POSE_ARRAY_CHANNEL = "pose_array";
            public static final String BASE_CONFIG_CHANNEL = "base_config";
            public static final String COMMAND_CHANNEL = "command";
            public static final String POSE_SHARE_CHANNEL = "pose_share_out";
            public static final String POSE_SHARE_PUB_COMPONENT = "pose_share_in";
            public static final String VELOCITY_PUBLISHER_CHANNEL = "vel_cmd";
        }
        public static final String SENSOR_NAME="collisionAvoid";
        public static final String AGENT_INDEX="agentIndex";
        public static final String TRANSPORT="rabbitmq";
        public static final String VELOCITY_QUEUE="velocityQueue";
        public static final String ODOM_BYTE_QUEUE = "odomQueue";

        public static final String LOCAL_IP_ARG="localIP";
        public static final String ROS_MASTER_URI="rosMasterUri";
        public static final String NO_SENSORS_ARG="numberSensors";
        public static final String SITES_ARG="sites";
    }

    public static class IotMsgContexts {
        public Map<String, IotRMQContext> Contexts = new HashMap<String, IotRMQContext>();

//        public IotMsgContexts(String sensorID) {
//            Contexts.put(Components.ODOMETRY_SPOUT_COMPONENT, new IotRMQContext(Components.ODOMETRY_SPOUT_COMPONENT, sensorID));
//            Contexts.put(Components.POSE_ARRAY_COMPONENT, new IotRMQContext(Components.POSE_ARRAY_COMPONENT, sensorID));
//            Contexts.put(Components.SCAN_COMPONENT, new IotRMQContext(Components.SCAN_COMPONENT, sensorID));
//            Contexts.put(Components.VELOCITY_COMMAND_PUBLISHER_COMPONENT,
//                    new IotRMQContext(Components.VELOCITY_COMMAND_PUBLISHER_COMPONENT, sensorID));
//            Contexts.put(Components.BASE_CONFIG_COMPONENT, new IotRMQContext(Components.BASE_CONFIG_COMPONENT, sensorID));
//            Contexts.put(Components.POSE_SHARE_PUB_COMPONENT, new IotRMQContext(Components.POSE_SHARE_PUB_COMPONENT, sensorID));
//            Contexts.put(Components.POSE_SHARE_COMPONENT, new IotRMQContext(Components.POSE_SHARE_COMPONENT, sensorID));
//        }

        public IotMsgContexts() {
            Contexts.put(Components.ODOMETRY_SPOUT_COMPONENT, new IotRMQContext(Components.ODOMETRY_SPOUT_COMPONENT));
            Contexts.put(Components.POSE_ARRAY_COMPONENT, new IotRMQContext(Components.POSE_ARRAY_COMPONENT));
            Contexts.put(Components.SCAN_COMPONENT, new IotRMQContext(Components.SCAN_COMPONENT));
            Contexts.put(Components.VELOCITY_COMMAND_PUBLISHER_COMPONENT,
                    new IotRMQContext(Components.VELOCITY_COMMAND_PUBLISHER_COMPONENT));
            Contexts.put(Components.COMMAND_SPOUT_COMPONENT, new IotRMQContext(Components.COMMAND_SPOUT_COMPONENT));
            Contexts.put(Components.POSE_SHARE_PUB_COMPONENT, new IotRMQContext(Components.POSE_SHARE_PUB_COMPONENT));
            Contexts.put(Components.POSE_SHARE_COMPONENT, new IotRMQContext(Components.POSE_SHARE_COMPONENT));
        }

    }

    public static abstract class Command {
        public static final String RESET_CMD = "reset";
        public static final String BASE_CONFIG_CMD = "baseConifg";
        public static final String DISCONNECTION_CMD = "disconnected";
        public static final String REACHED_GOAL="reachedGoal";
    }

    public static abstract class Misc {
        public static final long CHECK_ODOMETRY_TIMEOUT_INTERVAL = 5;//in seconds
        public static final long ODOMETRY_TIMEOUT = 5;//inseconds
    }
}
