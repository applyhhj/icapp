import thu.ic.collavoid.commons.planners.Methods_Planners;
import thu.ic.collavoid.commons.planners.Parameters;
import geometry_msgs.Pose;
import geometry_msgs.PoseWithCovarianceStamped;
import geometry_msgs.Quaternion;
import nav_msgs.Odometry;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.*;
import org.ros.node.topic.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thu.ic.collavoid.commons.rmqmsg.*;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by hjh on 2/6/15.
 */
public class scanto3dpointtest {
    private static Transform3D tfr0, tft0, tfr1, tft1;
    private static Logger logger = LoggerFactory.getLogger(scanto3dpointtest.class);

    public static void main(String[] args) throws URISyntaxException {
        String rosMaster = "http://149.160.204.248:11311";
        tstnode node = new tstnode("tst");
        NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic("156.56.93.238", new URI(rosMaster));
        nodeMainExecutor.execute(node, nodeConfiguration);
    }

    public static class tstnode extends AbstractNodeMain {
        private String nodename = "tst";
        private ConnectedNode node;

        public tstnode(String name) {
            nodename = name;
        }

        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of(nodename);
        }

        @Override
        public void onStart(ConnectedNode connectedNode) {
            Subscriber<PoseWithCovarianceStamped> efkposesub = connectedNode.newSubscriber("/robot_pose_ekf/odom_combined", PoseWithCovarianceStamped._TYPE);
//            Subscriber<PointCloud2> scanCloudSubscriber=connectedNode.newSubscriber("/robot0/scan_cloud",PointCloud2._TYPE);
            Subscriber<Odometry> odometrySubscriber0 = connectedNode.newSubscriber("/odom", Odometry._TYPE);
//            Subscriber<Odometry> odometrySubscriber1=connectedNode.newSubscriber("/robot1/odom",Odometry._TYPE);
//            Subscriber<Odometry> odometrySubscriber2=connectedNode.newSubscriber("/robot2/odom",Odometry._TYPE);
//            Subscriber<Odometry> odometrySubscriber3=connectedNode.newSubscriber("/robot3/odom",Odometry._TYPE);
//            final Publisher<Twist> velpub=connectedNode.newPublisher("/cmd_vel",Twist._TYPE);

            efkposesub.addMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
                @Override
                public void onNewMessage(PoseWithCovarianceStamped poseWithCovarianceStamped) {
                    Quaternion ori = poseWithCovarianceStamped.getPose().getPose().getOrientation();
                    System.out.println("efk ori: " + Methods_Planners.getYaw(new Vector4d_(ori.getX(), ori.getY(), ori.getZ(), ori.getW())) / Math.PI * 180);
                }
            });


//
//            setTransform();
//            connectedNode.executeCancellableLoop(new CancellableLoop() {
//                Scanner scanner = new Scanner(System.in);
//                boolean flag=true;
//                @Override
//                protected void loop() throws InterruptedException {
////                    String inputString = scanner.nextLine();
//                    if (scanner.nextLine().indexOf(" ")>=0){
//                    Twist vel=velpub.newMessage();
//                    vel.getAngular().setZ(0);
////                    velpub.publish(vel);
//                    Thread.sleep(50);
////                    vel.getLinear().setX(0);
////                    velpub.publish(vel);
////                        flag=false;
//                    }
//                }
//            });
            odometrySubscriber0.addMessageListener(new MessageListener<Odometry>() {
                @Override
                public void onNewMessage(Odometry odometry) {
                    Quaternion ori = odometry.getPose().getPose().getOrientation();
                    System.out.println("odo ori: " + Methods_Planners.getYaw(new Vector4d_(ori.getX(), ori.getY(), ori.getZ(), ori.getW())) / Math.PI * 180);
                }
            });
//
//            odometrySubscriber1.addMessageListener(new MessageListener<Odometry>() {
//                @Override
//                public void onNewMessage(Odometry odometry) {
//                    if (odometry.getTwist().getTwist().getLinear().getX()<0.45){
//                        System.out.println("linear speed rb1 < 0.45");
//                    }
//                }
//            });
//
//            odometrySubscriber2.addMessageListener(new MessageListener<Odometry>() {
//                @Override
//                public void onNewMessage(Odometry odometry) {
//                    if (odometry.getTwist().getTwist().getLinear().getX()<0.45){
//                        System.out.println("linear speed rb2< 0.45");
//                    }
//                }
//            });
//
//            odometrySubscriber3.addMessageListener(new MessageListener<Odometry>() {
//                @Override
//                public void onNewMessage(Odometry odometry) {
//                    if (odometry.getTwist().getTwist().getLinear().getX()<0.45){
//                        System.out.println("linear speed rb3 < 0.45");
//                    }
//                }
//            });

//            scanSubscriber.addMessageListener(new MessageListener<LaserScan>() {
//                @Override
//                public void onNewMessage(LaserScan laserScan) {
//                    System.out.print("\n scantype: ");
//                    float[] data=laserScan.getRanges();
//                    List<Double> datalist=new ArrayList<Double>();
//                    for (int i = 0; i < data.length; i++) {
//                        if (Double.isNaN(data[i]))
//                            continue;
//                        datalist.add(new Double(data[i]));
//                        System.out.print(data[i] + ",");
//                    }
//                    System.out.print("\n");
//                }
//            });
//
//            scanCloudSubscriber.addMessageListener(new MessageListener<PointCloud2>() {
//                @Override
//                public void onNewMessage(PointCloud2 pointCloud2) {
//                    System.out.println("\n pointcloud type: ");
//                    ChannelBuffer databuffer = pointCloud2.getData().copy();
//
//                    while (databuffer.readableBytes()>0){
//                        Vector3d_ point=new Vector3d_(
//                                databuffer.readFloat(),
//                                databuffer.readFloat(),
//                                databuffer.readFloat());
//                        double distance=Math.sqrt(point.getX()*point.getX()+point.getY()*point.getY());
//                        System.out.print(distance+",");
//                    }
//                    System.out.print("\n");
//                }
//            });
        }

        @Override
        public void onShutdown(Node node) {
            super.onShutdown(node);
        }
    }

    public static void setTransform() {
        double step = 2 * Math.PI / Parameters.ROBOT_NUMBER;
        Vector3d pose = new Vector3d(
                Parameters.POSE_RADIUS * Math.cos(0 * step),
                Parameters.POSE_RADIUS * Math.sin(0 * step),
                0);

        Double ori = Math.PI + 0 * step;

        Vector4d_ quat = Methods_Planners.getQuaternion(new Vector3d_(0, 0, 1), ori);
        Quat4d q = new Quat4d(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
        tft0 = new Transform3D(q, pose, 1);
//        System.out.println(q.toString());
        tfr0 = new Transform3D(q, new Vector3d(0, 0, 0), 1);

        Vector3d pose1 = new Vector3d(
                Parameters.POSE_RADIUS * Math.cos(1 * step),
                Parameters.POSE_RADIUS * Math.sin(1 * step),
                0
        );
        Double ori1 = Math.PI + 1 * step;

        Vector4d_ quat1 = Methods_Planners.getQuaternion(new Vector3d_(0, 0, 1), ori1);
        Quat4d q1 = new Quat4d(quat1.getX(), quat1.getY(), quat1.getZ(), quat1.getW());
        tft1 = new Transform3D(q1, pose1, 1);
        tfr1 = new Transform3D(q1, new Vector3d(0, 0, 0), 1);
    }

    public static Odometry_ toOdometry_(Odometry odometry, Transform3D tft, Transform3D tfr) {
        Pose_ pose_ = toPose_(odometry.getPose().getPose(), tft, tfr);

        Vector3d_ angular = new Vector3d_(
                odometry.getTwist().getTwist().getAngular().getX(),
                odometry.getTwist().getTwist().getAngular().getY(),
                odometry.getTwist().getTwist().getAngular().getZ()
        );
        Vector3d_ linear = new Vector3d_(
                odometry.getTwist().getTwist().getLinear().getX(),
                odometry.getTwist().getTwist().getLinear().getY(),
                odometry.getTwist().getTwist().getLinear().getZ()
        );
        Twist_ twist_ = new Twist_();
        twist_.setLinear(linear);
        twist_.setAngular(angular);

        Odometry_ odometry_ = new Odometry_();
        odometry_.setPose(pose_);
        odometry_.setTwist(twist_);
        odometry_.setChildFrameId(odometry.getChildFrameId());
        odometry_.getHeader().setFrameId(odometry.getHeader().getFrameId());
        odometry_.getHeader().setStamp(odometry.getHeader().getStamp().totalNsecs() / 1000000);
//        odometry_.setId(AgentID);
        return odometry_;
    }

    public static Pose_ toPose_(Pose pose, Transform3D tft, Transform3D tfr) {
        Point3d poseMap = new Point3d(
                pose.getPosition().getX(),
                pose.getPosition().getY(),
                pose.getPosition().getZ()
        );
//        System.out.println("--------pose in odom frame: "+poseMap.toString());
        tft.transform(poseMap);
//        System.out.println("========pose in map frame: "+poseMap.toString());
        Quat4d oriOdom = new Quat4d(
                pose.getOrientation().getX(),
                pose.getOrientation().getY(),
                pose.getOrientation().getZ(),
                pose.getOrientation().getW()
        );
//        System.out.println("--------ori in odom frame: "+oriOdom.toString());
        Quat4d oriMap = new Quat4d();
        Transform3D tfrOdom = new Transform3D(oriOdom, new Vector3d(0, 0, 0), 1);
        tfrOdom.mul(tfr);
        tfrOdom.get(oriMap);
//        System.out.println("========ori in map frame: " + oriMap.toString());
        Pose_ pose_ = new Pose_();
        Vector3d_ position = new Vector3d_(
                poseMap.x,
                poseMap.y,
                poseMap.z
        );
        Vector4d_ orientation = new Vector4d_(
                oriMap.x,
                oriMap.y,
                oriMap.z,
                oriMap.w
        );
        pose_.setOrientation(orientation);
        pose_.setPosition(position);
        return pose_;
    }
}
