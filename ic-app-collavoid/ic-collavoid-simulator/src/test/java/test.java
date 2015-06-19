import nav_msgs.Odometry;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.*;
import org.ros.node.topic.Subscriber;
import simbad.demo.BlinkingLampDemo;
import simbad.gui.Simbad;

import java.net.URI;
import java.net.URISyntaxException;

public class test {
    static private Subscriber<Odometry> odometrySubscriber;
    static private TestNode tstnode;
    static private NodeMainExecutor nodeMainExecutor;
    public static void main(String[] args) throws URISyntaxException{
        NodeConfiguration nodeConfiguration;
        String rosMaster="http://localhost:11311";
        String localIp="localhost";
//        try {
            nodeConfiguration = NodeConfiguration.newPublic(localIp, new URI(rosMaster));
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
        tstnode=new TestNode("robot0");
        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
        nodeMainExecutor.execute(tstnode, nodeConfiguration);

    }
    
    public static class TestNode extends AbstractNodeMain{
        String nodeName;
        String topname;
        public TestNode(String name){
            topname=name;
            nodeName=name+"_tst";
        }
        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of(nodeName);
        }

        @Override
        public void onStart(ConnectedNode connectedNode) {
            System.out.println("node started");
            odometrySubscriber =
                    connectedNode.newSubscriber(topname + "/odom", Odometry._TYPE);

            odometrySubscriber.addMessageListener(new MessageListener<Odometry>() {

                @Override
                public void onNewMessage(Odometry odometry) {
                    System.out.println("------received odom");
                }
            });
        }
    }
}
