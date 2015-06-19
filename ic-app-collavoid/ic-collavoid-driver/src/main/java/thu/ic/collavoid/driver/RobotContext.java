package thu.ic.collavoid.driver;

import thu.ic.collavoid.commons.rmqmsg.Twist_;
import org.ros.node.NodeConfiguration;
import thu.instcloud.client.core.StreamProducer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by hjh on 6/14/15.
 */
public class RobotContext {
    
    private String robotIp;    
    private String rosMasterUrl;
    private String robotId;
    private String controllerRosNodeName;
    private int robotIdx;
    
    private Map<String,StreamProducer> produces;
    private BlockingQueue<Twist_> velocityQueue;
    
    public RobotContext(int robotIdx,String robotIp,String rosMasterIp){
        this.robotIdx=robotIdx;
        this.robotIp=robotIp;
        this.rosMasterUrl =rosMasterIp;
        
        this.robotId=Constants.DEFAULT_ROBOT_NAME+robotIdx;
        this.controllerRosNodeName=robotId+Constants.CONTROLLER_NODE_NAME_SUFFIX;
        this.velocityQueue=new LinkedBlockingQueue<>();
    }

    public void setProduces(Map<String, StreamProducer> produces) {
        this.produces = produces;
    }

    public void setVelocityQueue(BlockingQueue<Twist_> velocityQueue) {
        this.velocityQueue = velocityQueue;
    }   

    public BlockingQueue<Twist_> getVelocityQueue() {
        return velocityQueue;
    }

    public String getControllerRosNodeName() {
        return controllerRosNodeName;
    }

    public String getRobotId() {
        return robotId;
    }

    public int getRobotIdx() {
        return robotIdx;
    }

    public Map<String, StreamProducer> getProduces() {
        return produces;
    }
    
    public NodeConfiguration getRosNodeConfiguration() throws URISyntaxException{
        return NodeConfiguration.newPublic(robotIp, new URI(rosMasterUrl));
        
    }
}
