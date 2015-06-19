package thu.ic.collavoid.commons.TimeDelayAnalysis;

public abstract class Constants {
    public static final String basedir = "/home/hjh/test/";
    public static final String PARAMETER_DELAY = "ParameterDelay";
    public static final String COMPUTATION_DELAY = "CMDDelay";
    public static final String TIME_TAG = "TimeTag";
    
    public static final String localRMQUrl="amqp://149.165.159.12:5672";
    public static final String remoteRMQUrl="amqp://149.165.159.12:5672";
//    public static final String localRMQUrl="amqp://localhost:5672";
//    public static final String remoteRMQUrl="amqp://localhost:5672";
    public static final String exchangeName="delayTest";
    public static final String exchangeType="topic";
    public static final String routingKey="delayKey";

    // computation delay keys
    public static final String AGENT_STATE_DELAY="agentStateDelay";
    public static final String POSE_SHARE_DELAY="poseShareDelay";
    public static final String VELOCITY_COMPUTE_DELAY="velocityComputeDelay";
    public static final String CHECK_EMIT_DELAY="checkEmit";

    // communication delay keys
//    public static final String COMM_POSE_SHARE_DELAY="commPoseShareDelay";
    public static final String COMM_COMPUTE_DELAY="commComputeDelay";
    public static final String ROBOT_NUMBER_KEY="robotNumber";
}
