import cgl.iotcloud.core.SensorContext;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class AgentControllerIot {
    private SensorContext context;
    private AgentROSNodeIot agentROSNode;
    private String AgentID;
    private NodeMainExecutor nodeMainExecutor;

    public AgentControllerIot(SensorContext context) {
        this.context = context;
        this.AgentID = "robot1";
    }

    public void start(NodeConfiguration configuration) {
        //need to be a different node name
        agentROSNode = new AgentROSNodeIot(AgentID + "_rmq", context);
        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
        nodeMainExecutor.execute(agentROSNode, configuration);

    }
}
