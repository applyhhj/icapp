package thu.ic.collavoid.driver;

import thu.ic.collavoid.commons.planners.Parameters;
import thu.ic.collavoid.commons.rmqmsg.*;
import thu.ic.collavoid.commons.storm.Constant_storm;
import com.esotericsoftware.kryo.Kryo;
import geometry_msgs.Pose;
import geometry_msgs.PoseArray;
import geometry_msgs.Twist;
import nav_msgs.Odometry;
import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sensor_msgs.PointCloud2;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RobotRosNode extends AbstractNodeMain {

    private Logger logger= LoggerFactory.getLogger(RobotRosNode.class);
    private ReadWriteLock rwl = new ReentrantReadWriteLock();
    private RobotContext context;
    private BlockingQueue<Twist_> velQueue;
    private BlockingQueue<Boolean> goalReachedQueue;
    private Publisher<Twist> velcmdPublisher;
    private Subscriber<Odometry> odometrySubscriber;
    private Subscriber<PointCloud2> laserScanSubscriber;
    private Subscriber<PoseArray> poseArraySubscriber;
    private Subscriber<std_msgs.String> cmdSubscriber;
    private Publisher<std_msgs.String> cmdPublisher;
    private Odometry_ currentOdom = null;
    private BaseConfig_ baseConfig;
    private long seq;
    private boolean repeat=false;
    
    //test
    private Publisher<Twist> velcmdPublisherGazebo;

    public RobotRosNode(RobotContext context) {
        this.context=context;
        // running controller node need a different node name however topics and other stuffs are
        // using original node name which is also used as the sensorid. So need to get rid of the suffix.
        this.velQueue=context.getVelocityQueue();
        this.goalReachedQueue = new LinkedBlockingDeque<>();
        this.baseConfig = new BaseConfig_();
        seq=System.currentTimeMillis();
        baseConfig.setPublisMeFreq(Parameters.PUBLISH_ME_FREQUENCY);
        baseConfig.setControlFreq(Parameters.CONTROLLER_FREQUENCY);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        velcmdPublisherGazebo=
                connectedNode.newPublisher(context.getRobotId() + "/commands/velocity", Twist._TYPE);
        velcmdPublisher =
                connectedNode.newPublisher(context.getRobotId() + "/cmd_vel", Twist._TYPE);
        odometrySubscriber =
                connectedNode.newSubscriber(context.getRobotId() + "/odometry", Odometry._TYPE);
        laserScanSubscriber =
                connectedNode.newSubscriber(context.getRobotId() + "/scan/point_cloud2", PointCloud2._TYPE);
        poseArraySubscriber =
                connectedNode.newSubscriber(context.getRobotId() + "/particlecloud", PoseArray._TYPE);
        cmdSubscriber =
                connectedNode.newSubscriber(context.getRobotId() + "/ctl_cmd", std_msgs.String._TYPE);
        cmdPublisher=
                connectedNode.newPublisher(context.getRobotId()+"/ctl_cmd",std_msgs.String._TYPE);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private Kryo kryo = Methods_RMQ.getKryo();
            @Override
            protected void loop() throws InterruptedException {
                goalReachedQueue.take();
                Odometry_ odometry_ = getCurrentOdom();
                while (odometry_ == null
                        || odometry_.getTwist().getLinear().length() > 0
                        || odometry_.getTwist().getAngular().length() > 0) {
                    Twist_ cmd=new Twist_();
                    cmd.setSeq(seq);
                    velQueue.offer(cmd);
                    Thread.sleep(100);
                    odometry_ = getCurrentOdom();
                }
                Map<String, Object> prop = new HashMap<>();
                seq=System.currentTimeMillis();
                setStartGoal();
                prop.put(Constant_storm.FIELDS.TIME_FIELD, baseConfig.getTime());
                prop.put(Constant_storm.IC.AGENT_INDEX,context.getRobotIdx());
                context.getProduces().get(Constant_storm.IC.channels.COMMAND_CHANNEL)
                        .send(Methods_RMQ.serialize(kryo, baseConfig), prop);
                System.out.println(context.getRobotId() + " published goal!");
            }
        });

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void loop() throws InterruptedException {
                Twist_ m = velQueue.take();
                if (m.getSeq()==seq){
                    Twist cmdROS = velcmdPublisher.newMessage();
                    cmdROS.getLinear().setX(m.getLinear().getX());
                    cmdROS.getLinear().setY(m.getLinear().getY());
                    cmdROS.getLinear().setZ(m.getLinear().getZ());
                    cmdROS.getAngular().setX(m.getAngular().getX());
                    cmdROS.getAngular().setY(m.getAngular().getY());
                    cmdROS.getAngular().setZ(m.getAngular().getZ());
                    velcmdPublisher.publish(cmdROS);
                    velcmdPublisherGazebo.publish(cmdROS);
                    if (m.isGoalReached()&&repeat) {
                        goalReachedQueue.offer(m.isGoalReached());
                        if (cmdPublisher!=null){
                            std_msgs.String msg=cmdPublisher.newMessage();
                            msg.setData(Constant_storm.Command.REACHED_GOAL);
                            cmdPublisher.publish(msg);
                            cmdPublisher.shutdown();
                            cmdPublisher=null;
                        }
                    }
                }
            }
        });

        odometrySubscriber.addMessageListener(new MessageListener<Odometry>() {
            private Kryo kryo = Methods_RMQ.getKryo();
            @Override
            public void onNewMessage(Odometry odometry) {
                Map<String,Object> prop=new HashMap<String, Object>();
                Odometry_ odometry_=toOdometry_(odometry);
                setCurrentOdom(odometry_);
                prop.put(Constant_storm.FIELDS.TIME_FIELD,odometry_.getHeader().getStamp());
                prop.put(Constant_storm.IC.AGENT_INDEX,context.getRobotIdx());
                context.getProduces().get(Constant_storm.IC.channels.ODOMETRY_CHANNEL)
                        .send(Methods_RMQ.serialize(kryo, odometry_),prop);                                
            }
        });

        laserScanSubscriber.addMessageListener(new MessageListener<PointCloud2>() {
            private Kryo kryo = Methods_RMQ.getKryo();
            @Override
            public void onNewMessage(PointCloud2 pointCloud2) {
                Map<String,Object> prop=new HashMap<String, Object>();
                PointCloud2_ pointCloud2_=toPointCloud2_(pointCloud2);
                prop.put(Constant_storm.FIELDS.TIME_FIELD,pointCloud2_.getHeader().getStamp());
                prop.put(Constant_storm.IC.AGENT_INDEX,context.getRobotIdx());
                context.getProduces().get(Constant_storm.IC.channels.SCAN_CHANNEL)
                        .send(Methods_RMQ.serialize(kryo, pointCloud2_),prop);
            }
        });

        poseArraySubscriber.addMessageListener(new MessageListener<PoseArray>() {
            private Kryo kryo = Methods_RMQ.getKryo();
            @Override
            public void onNewMessage(PoseArray poseArray) {
                Map<String,Object> prop=new HashMap<String, Object>();
                PoseArray_ poseArray_=toPoseArray_(poseArray);
                prop.put(Constant_storm.FIELDS.TIME_FIELD,poseArray_.getHeader().getStamp());
                prop.put(Constant_storm.IC.AGENT_INDEX,context.getRobotIdx());
                context.getProduces().get(Constant_storm.IC.channels.POSE_ARRAY_CHANNEL)
                        .send(Methods_RMQ.serialize(kryo, poseArray_),prop);
            }
        });

        cmdSubscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String string) {
                if (string.getData().equals(Constant_storm.Command.RESET_CMD)) {
                    System.out.println(context.getRobotId() + " received reset cmd");
                    goalReachedQueue.offer(true);
                }
            }
        });
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    private void setStartGoal() {
        Odometry_ currentOdom;
        do {
            currentOdom = getCurrentOdom();
        } while (currentOdom==null);

        PoseStamped_ start = new PoseStamped_();
        PoseStamped_ goal = new PoseStamped_();
        start.getPose().setPosition(new Vector3d_(
                        currentOdom.getPose().getPosition().getX(),
                        currentOdom.getPose().getPosition().getY(),
                        currentOdom.getPose().getPosition().getZ())
        );
        goal.getPose().setPosition(new Vector3d_(
                        -currentOdom.getPose().getPosition().getX(),
                        -currentOdom.getPose().getPosition().getY(),
                        -currentOdom.getPose().getPosition().getZ())
        );
        Quat4d oriStart = new Quat4d(
                currentOdom.getPose().getOrientation().getX(),
                currentOdom.getPose().getOrientation().getY(),
                currentOdom.getPose().getOrientation().getZ(),
                currentOdom.getPose().getOrientation().getW()
        );
        Quat4d oriGoal = new Quat4d();
        Transform3D tfr = new Transform3D(oriStart, new Vector3d(0, 0, 0), 1);
        Transform3D tfrPI = new Transform3D(new Quat4d(0, 0, 1, 0), new Vector3d(), 1);
        tfr.mul(tfrPI);
        tfr.get(oriGoal);

        start.getPose().setOrientation(new Vector4d_(oriStart.x, oriStart.y, oriStart.z, oriStart.w));
        goal.getPose().setOrientation(new Vector4d_(oriGoal.x, oriGoal.y, oriGoal.z, oriGoal.w));
        baseConfig.setStart(start);
        baseConfig.setGoal(goal);
        baseConfig.setTime(System.currentTimeMillis());
        baseConfig.setSeq(seq);
    }

    private void setCurrentOdom(Odometry_ odometry_) {
        rwl.writeLock().lock();
        try {
            this.currentOdom = odometry_;
        } finally {
            rwl.writeLock().unlock();
        }
    }

    public Odometry_ getCurrentOdom() {
        rwl.readLock().lock();
        try {
            if (currentOdom == null||currentOdom.getHeader().getSeq()!=seq) {
                return null;
            } else {
                return currentOdom.copy();
            }
        } finally {
            rwl.readLock().unlock();
        }
    }

    private Odometry_ toOdometry_(Odometry odometry) {
        Pose_ pose_ = toPose_(odometry.getPose().getPose());

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
        odometry_.getHeader().setSeq(seq);
        return odometry_;
    }

    private PointCloud2_ toPointCloud2_(PointCloud2 pointCloud2) {
        PointCloud2_ pts = new PointCloud2_();
        pts.setWidth(pointCloud2.getWidth());
        pts.setHeight(pointCloud2.getHeight());
        pts.setDimension(pointCloud2.getFields().size());
        int pointNumber = pts.getWidth() * pts.getHeight();
        double[] data;
        if (pointNumber == 0)
            data = new double[0];
        else {
            data = new double[pointNumber * 3];
            int i = 0;
            ChannelBuffer databuffer = pointCloud2.getData().copy();
            while (databuffer.readableBytes() > 0)
                data[i++] = (double) databuffer.readFloat();
        }
        pts.setData(data);
        pts.getHeader().setStamp(pointCloud2.getHeader().getStamp().totalNsecs() / 1000000);
        pts.getHeader().setFrameId(pointCloud2.getHeader().getFrameId());
        pts.getHeader().setSeq(seq);
        return pts;
    }

    private PoseArray_ toPoseArray_(PoseArray poseArray) {
        PoseArray_ pa = new PoseArray_();
        pa.getHeader().setFrameId(poseArray.getHeader().getFrameId());
        pa.getHeader().setStamp(poseArray.getHeader().getStamp().totalNsecs() / 1000000);
        pa.getHeader().setSeq(seq);

        List<Pose_> poses = new ArrayList<Pose_>();
        for (Pose pose : poseArray.getPoses()) {
            poses.add(toPose_(pose));
        }
        pa.setPoses(poses);
        return pa;
    }

    private Pose_ toPose_(Pose pose) {
        Pose_ pose_ = new Pose_();
        Vector3d_ position = new Vector3d_(
                pose.getPosition().getX(),
                pose.getPosition().getY(),
                pose.getPosition().getZ()
        );
        Vector4d_ orientation = new Vector4d_(
                pose.getOrientation().getX(),
                pose.getOrientation().getY(),
                pose.getOrientation().getZ(),
                pose.getOrientation().getW()
        );
        pose_.setOrientation(orientation);
        pose_.setPosition(position);
        return pose_;
    }

    @Override
    public void onShutdown(Node node) {
        velcmdPublisher.shutdown();
        odometrySubscriber.shutdown();
        poseArraySubscriber.shutdown();
        cmdSubscriber.shutdown();
        laserScanSubscriber.shutdown();
        node.shutdown();
    }

    @Override
    public GraphName getDefaultNodeName() {
        // should be the same as node name or will not work right
        return GraphName.of(this.context.getControllerRosNodeName());
    }
}