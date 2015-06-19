package thu.ic.collavoid.driver;

import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeMainExecutor;

import java.net.URISyntaxException;

public class RobotController {
    private RobotContext context;
    private RobotRosNode agentROSNode;
    private NodeMainExecutor nodeMainExecutor;
    

    public RobotController(RobotContext context){
        this.context=context;
    }

    public void start() {
        //need to be a different node name
        agentROSNode = new RobotRosNode(context);
        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
        try {
            nodeMainExecutor.execute(agentROSNode, context.getRosNodeConfiguration());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }
    
    public void stop(){
        nodeMainExecutor.shutdown();
    }
}
