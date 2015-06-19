package thu.ic.collavoid.simulator;

import thu.ic.collavoid.commons.planners.Parameters;
import thu.ic.collavoid.commons.storm.Constant_storm;
import com.rabbitmq.client.Address;
import geometry_msgs.Pose;
import geometry_msgs.PoseArray;
import geometry_msgs.Twist;
import nav_msgs.Odometry;
import org.apache.commons.cli.*;
import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.node.ConnectedNode;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import sensor_msgs.PointCloud2;
import simbad.gui.Simbad;
import simbad.sim.*;

import javax.media.j3d.Transform3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulatorIC {
    static int robotNb = 4;
    static double posRadius = Parameters.POSE_RADIUS;
    static boolean bgMode=false;
    static List<Robot> robots = new ArrayList<Robot>();
    static String fname="test";
//    private static DataRecorder delayRecorder,collisionRecorder,computeDelayRecorder;
//    private static boolean beginRecording=false;

    private static final String ROBOT_NUMBER_IPT_NAME ="n";
    private static final String ROBOT_NUMBER_KEY="robotNumber";
    private static final String ROBOT_POSE_RADIUS_IPT_NAME="r";
    private static final String ROBOT_POSE_RADIUS_KEY="robotPoseRadius";
    private static final String FILE_NAME_IPT_NAME="f";
    private static final String FILE_NAME_KEY="fname";
    private static final String BACKGROUND_MODE_IPT_NAME="b";
    private static final String BACKGROUND_MODE_KEY="backGroundMode";

    //test
    static int reachGoalNo;
    static int startedNo;

    //test

    public static class WheelVelocity {
        private double vl;
        private double vr;
        private boolean isNew;
        public void setVl(double vl) {
            this.vl = vl;
        }
        public void setVr(double vr) {
            this.vr = vr;
        }
        public void setIsNew(boolean state) {
            isNew = state;
        }
        public double getVl() {
            return vl;
        }
        public double getVr() {
            return vr;
        }
        public void reset() {
            vl = 0;
            vr = 0;
        }

        public boolean getIsNew() {
            return isNew;
        }

    }

    /**
     * Describe the robot
     */
    static public class Robot extends Agent {

        //test
        long lastnewvel;
        boolean startAgain;
        //test

        //id
        int id;
        String robotName;

        //for shutdown
        AgentNode agentNode;

        //orientation
        double orientation;
        //kinematic
        DifferentialKinematic kinematic;
        private double wheelDistance;

        //sensors
        LaserScan laserScan;
        RangeSensorBelt sensors;
//        RangeSensorBelt bumpers;

        //node
        private ConnectedNode node;
        private ParameterTree params;

        //start and goal pubisher
//        Publisher<PoseStamped> startGoalPublisher = null;
//        PoseStamped startGoalMsg;
//        Pose start, goal;
//        int sgseq;

        //odometry publisher
        Publisher<Odometry> odometryPublisher = null;
        Odometry odomMsg;
        int odomSeq;

        //laser scan publisher
        Publisher<PointCloud2> laserscanPublisher = null;
        PointCloud2 pc2, pctmp;
        int pc2Seq;

        //velocity command subscriber
        Subscriber<Twist> velocitySubscriber = null;
        WheelVelocity wheelVelocity = new WheelVelocity();

        private double vl, vr;

        // control command subscriber
        Publisher<std_msgs.String> ctlCmdPublisher = null;
        Subscriber<std_msgs.String> ctlCmdSubscriber=null;
        std_msgs.String ctlCmd;
        boolean reached=false;

        //frame
        String robotFrame;
        String odomFrame;
        String globalFrame;

        //for synchronization
        Time time = new Time();

        //pose array publisher, currently for test
        Publisher<PoseArray> poseArrayPublisher = null;
        PoseArray poseArray;
        int paSeq;
        Point3d previousPosition = null;

        LampActuator lamp;
        int lamponcnt;

        public Robot(Vector3d position, double ori, int id) {
            // initialize position and orientation
            super(position, "robot" + id);
            this.id = id;
            this.robotName = "robot" + id;
            this.radius = (float) (Parameters.FOOTPRINT_RADIUS);// loaded from agent parameters
            orientation = ori;
            lamp = RobotFactory.addLamp(this);
            //use differential model
            kinematic = RobotFactory.setDifferentialDriveKinematicModel(this);
            wheelDistance = this.getRadius();
            // Add camera
            //camera = RobotFactory.addCameraSensor(this);
            laserScan = new LaserScan(this.radius,
                    SimParams.SCAN_ANGLE_RANGE / 180 * Math.PI,
                    SimParams.SCAN_SENSOR_NB,
                    SimParams.SCAN_MIN_RANGE,
                    SimParams.SCAN_MAX_RANGE,
                    SimParams.SCAN_UPDATE_FREQ);
            sensors = laserScan.getSensor();
//            bumpers = RobotFactory.addBumperBeltSensor(this, 12);
            // if sensors are not on the center of the robot then height
            // should be assigned to the laserscan, in the robot frame
            this.addSensorDevice(sensors, new Vector3d(0, 0, 0), 0);
            //initialize frames
            robotFrame = this.getName() + "_base";
            odomFrame = this.getName() + "_odometry";
            globalFrame = "map";
            
            initPubSub();
//            initController(
//                    "robot" + id,
//                    null,
//                    Constant_msg.RMQ_URL
//            );
        }

        private void initController(String name, Address[] addresses, String url) {
//            agentController = new AgentControllerStorm(name, addresses, url);
//            NodeConfiguration configuration = NodeConfiguration.newPublic("localhost");
//            agentController.start(configuration);
        }

        public void initPubSub() {
            // initialize node
            String ROS_IP="localhost";
            String ROS_MASTER_RUI="http://localhost:11311";
            agentNode = new AgentNode(this.getName(),ROS_IP,ROS_MASTER_RUI);
            node = agentNode.getNode();
            // publish robot numbers to setup planner numbers
            params = node.getParameterTree();
            params.set("robotNb", robotNb);
            params.set("posRadius", posRadius);
            // initialize odometry publisher and message
            if (odometryPublisher == null) {
                odometryPublisher = node.newPublisher(this.getName() + "/odometry", Odometry._TYPE);
                // initialize message
                odomMsg = odometryPublisher.newMessage();
                odomMsg.getHeader().setFrameId(odomFrame);
                odomMsg.setChildFrameId(robotFrame);
                odomSeq = 0;
            }
            //initialize laser scan publisher
            if (laserscanPublisher == null) {
                laserscanPublisher = node.newPublisher(this.getName() + "/scan/point_cloud2", PointCloud2._TYPE);
                // initialize message
                pctmp = laserscanPublisher.newMessage();
                pc2 = laserscanPublisher.newMessage();
                pc2.getHeader().setFrameId(globalFrame);
                pc2Seq = 0;
            }

            //for test
            if (poseArrayPublisher == null) {
                poseArrayPublisher = node.newPublisher(this.getName() + "/particlecloud", PoseArray._TYPE);
                poseArray = node.getTopicMessageFactory().newFromType(PoseArray._TYPE);
                poseArray.getHeader().setFrameId(robotFrame);
                paSeq = 0;
            }

            //initialize velocity command subscriber
            if (velocitySubscriber == null) {
                velocitySubscriber = node.newSubscriber(this.getName() + "/cmd_vel", Twist._TYPE);
                velocitySubscriber.addMessageListener(new MessageListener<Twist>() {
                    int cnt = 0, winsize = 20;
                    long delay;
                    double delay_avg;
                    int ignoreTimes=0;//ignore first ten times
                    List<String> dataContent=new ArrayList<String>();
                    @Override
                    public void onNewMessage(Twist msg) {

//                        delay = delay + System.currentTimeMillis() - lastnewvel;
//                        lastnewvel = System.currentTimeMillis();
//                        if (cnt++ % winsize == 0) {
////                            System.out.println(robotName+" Average velocity command delay: " + (double) delay / winsize);
//                            delay_avg=(double) delay / winsize;
//                            ignoreTimes++;
//                            if (!beginRecording&&delay_avg<5000&&ignoreTimes>10){
//                                beginRecording=true;
//                            }
//                            if (beginRecording) {
//                                dataContent.clear();
//                                dataContent.add(robotName);
//                                dataContent.add(delay_avg + "");
//                                delayRecorder.append(dataContent);
//                            }
//                            delay = 0;
//                        }

                        double v, w;
                        //in ros coordinate
                        Vector3d vel = new Vector3d(msg.getLinear().getX(), msg.getLinear().getY(), msg.getLinear().getZ());
                        //no need to transform the coordinate
                        v = vel.length();
                        w = msg.getAngular().getZ();

                        vl = v - w * wheelDistance / 2;
                        vr = v + w * wheelDistance / 2;

                        //test
//                       if (id==0)
//                      System.out.println("robot"+id+" New cmd delay: "+(System.currentTimeMillis()-lastnewvel));
//                        wheelVelocity.setVl(v - w * wheelDistance / 2);
//                        wheelVelocity.setVr(v + w * wheelDistance / 2);
//                        wheelVelocity.setIsNew(true);
//                        if (agentController.isGoalReached()) {
//                            reachGoalNo++;
//                            startAgain = true;
//                            startedNo = 0;
//                        }
                        //test

                    }
                }, 1);
            }

//            if (startGoalPublisher == null) {
//                startGoalPublisher = node.newPublisher(this.getId() + "/start_goal", PoseStamped._TYPE);
//                startGoalMsg = startGoalPublisher.newMessage();
//                startGoalMsg.getHeader().setFrameId(globalFrame);
//                start = utilsSim.messageFactory.newFromType(Pose._TYPE);
//                goal = utilsSim.messageFactory.newFromType(Pose._TYPE);
//                sgseq = 0;
//            }

            if (ctlCmdPublisher == null) {
                ctlCmdPublisher = node.newPublisher(this.getName() + "/ctl_cmd", std_msgs.String._TYPE);
                ctlCmd = ctlCmdPublisher.newMessage();
            }
            if (ctlCmdSubscriber==null){
                ctlCmdSubscriber=node.newSubscriber(this.getName() + "/ctl_cmd", std_msgs.String._TYPE);
                ctlCmdSubscriber.addMessageListener(new MessageListener<std_msgs.String>() {
                    @Override
                    public void onNewMessage(std_msgs.String string) {
                        String msg=string.getData();
                        if (msg.equals(Constant_storm.Command.REACHED_GOAL)){
                            reached=true;
                            ctlCmdSubscriber.shutdown();
                        }
                    }
                });
            }
        }

        /**
         * This method is called by the simulator engine on startAgain.
         */
        public void initBehavior() {
//            agentController.clearQueues();
            this.resetPosition();
            this.rotateY(orientation);
//            wheelVelocity.reset();
            vl = 0;
            vr = 0;
            lamp.setOn(false);
            ctlCmd.setData(Constant_storm.Command.RESET_CMD);
            System.out.println(this.getName() + "send out reset cmd");
            ctlCmdPublisher.publish(ctlCmd);
            setRobotColor();
        }
        
        private void setRobotColor(){
            if (this.id%3==0){
                float colorvalue = (float) this.id / robotNb;
                setColor(new Color3f(colorvalue, 0, 0));
            }else if (this.id%3==1){
                float colorvalue = (float) this.id / robotNb;
                setColor(new Color3f(0,colorvalue , 0));
            }else {
                float colorvalue = (float) this.id / robotNb;
                setColor(new Color3f(0,  0,colorvalue));
            }

        }

        /**
         * This method is call cyclically (20 times per second)  by the simulator engine.
         */
        public void performBehavior() {
            // send out goal
//            if (ctlCmd.equals("pause") || sgseq < 10) {
//                kinematic.setWheelsVelocity(0, 0);
//            } else {
                kinematic.setWheelsVelocity(vl, vr);
//                setKinematic(wheelVelocity);
//            }

//            time = node.getCurrentTime();
            // use java system time to make sure time are synchronized
            time = Time.fromMillis(System.currentTimeMillis());

            //publish scan in frequency of 10Hz
            if (getCounter() % 1 == 0) {
                pc2.getHeader().setSeq(pc2Seq++);
                pc2.getHeader().setStamp(time);
                //publish valid laser scan in pointcloud2 format in global frame
                laserScan.getLaserscanPointCloud2(pc2, this.getTransform());
                laserscanPublisher.publish(pc2);
            }
            //test publish localization pose array
            if (getCounter() % 2 == 0) {
                Point3d cor = new Point3d();
                this.getCoords(cor);
                poseArray.getHeader().setStamp(time);
                poseArray.getHeader().setSeq(paSeq++);
                if (previousPosition == null || !cor.equals(previousPosition)) {
                    if (previousPosition == null)
                        previousPosition = new Point3d();
                    this.getCoords(previousPosition);
                    setPoseArrayMsg();
                }
                poseArrayPublisher.publish(poseArray);
            }
            //send odometry in frequency of 10 HZ
            if (getCounter() % 1 == 0) {
                setOdomMsg(time);
                odometryPublisher.publish(odomMsg);
            }

//            if (sgseq < 10) {
//
//                getStartGoal();
//                startGoalMsg.getHeader().setStamp(time);
//                startGoalMsg.getHeader().setSeq(sgseq);
//
//                // set start in even sequence
//                if (sgseq % 2 == 0)
//                    startGoalMsg.setPose(start);
//                else
//                    startGoalMsg.setPose(goal);
//                startGoalPublisher.publish(startGoalMsg);
//                sgseq++;
//            }

            //test
//            if (reachGoalNo >= robotNb && startAgain) {
//                sgseq = 0;
//                this.startAgain = false;
//                if (++startedNo >= robotNb)
//                    reachGoalNo = 0;
////                System.out.println(robotName+"reached goal");
//            }
//            recordHit();

            //test

        }

        private void recordHit(){
            if (this.anOtherAgentIsVeryNear()) {
                if (!lamp.getOn()){
                    List<String> collInfo=new ArrayList<>();
                    collInfo.add(robotName + " detected collision with "+this.getVeryNearAgent().getName());
                    System.out.println(collInfo.get(0));
                    if (reached){
//                        collisionRecorder.append(collInfo);
                    }
                }
                lamp.setOn(true);
            }else {
                if (lamp.getOn()){
                    lamp.setOn(false);
                }
            }
        }

        private void checkHit() {
            if (this.anOtherAgentIsVeryNear()) {
                if (!lamp.getOn()){
                    List<String> collInfo=new ArrayList<>();
                    collInfo.add(robotName + " detected collision with "+this.getVeryNearAgent().getName());
                    System.out.println(collInfo.get(0));
//                    collisionRecorder.append(collInfo);
                }
                lamp.setOn(true);
                lamponcnt = 30;
            }
            if (lamp.getOn() && lamponcnt >= 0)
                lamponcnt--;
            if (lamponcnt < 0)
                lamp.setOn(false);
        }

        private void setKinematic(WheelVelocity wv) {
            double vln, vrn;
            if (wv.getIsNew()) {
                vln = setv(vl, wv.getVl());
                vrn = setv(vr, wv.getVr());
                kinematic.setWheelsVelocity(vln, vrn);
                wv.setIsNew(false);
            } else {
                vln = setv(vl, 0);
                vrn = setv(vr, 0);
                kinematic.setWheelsVelocity(vln, vrn);
            }
            vl = vln;
            vr = vrn;
        }

        private double setv(double a, double b) {
            double sign = Math.signum(b - a);
            return sign * Math.min(b * sign, sign * (a + sign * Parameters.ACC_LIM_X / Parameters.CONTROLLER_FREQUENCY / 5));
        }

//        private void getStartGoal() {
//            Point3d start_cor = new Point3d();
//            this.getCoords(start_cor);
//            Point3d goal_cor = new Point3d();
//            goal_cor.set(-start_cor.getX(), start_cor.getY(), -start_cor.getZ());
//
//            Quat4d oriStart = getOrientation();
//            Quat4d oriGoal = new Quat4d();
//            Transform3D tfr = new Transform3D(oriStart, new Vector3d(0, 0, 0), 1);
//            Transform3D tfrPI = new Transform3D(new Quat4d(0, 1, 0, 0), new Vector3d(), 1);
//            tfr.mul(tfrPI);
//            tfr.get(oriGoal);
//
//            // transform coordinates
//            start = utilsSim.getPose(start_cor, oriStart);
//            goal = utilsSim.getPose(goal_cor, oriGoal);
//        }

        public void setOdomMsg(Time t) {
            //get position and velocities, all in odometry frame
            Point3d cor = new Point3d();
            this.getCoords(cor);
            Quat4d ori = getOrientation();

            odomMsg.getHeader().setStamp(t);
            odomMsg.getHeader().setSeq(odomSeq++);

            odomMsg.getPose().setPose(utilsSim.getPose(cor, ori));
            odomMsg.getTwist().getTwist().setLinear(utilsSim.getTwist(this.linearVelocity));
            odomMsg.getTwist().getTwist().setAngular(utilsSim.getTwist(this.angularVelocity));

        }

        public Quat4d getOrientation() {
            //get orientation
            Transform3D tfr = new Transform3D();
            Quat4d ori = new Quat4d();
            this.getRotationTransform(tfr);
            tfr.get(ori);
            return ori;
        }


        public Transform3D getTransform() {
            Transform3D tf = new Transform3D();
            Vector3d tft3d = new Vector3d();
            Quat4d tfrq = new Quat4d();
            this.getTranslationTransform(tf);
            tf.get(tft3d);
            this.getRotationTransform(tf);
            tf.get(tfrq);
            tf.set(tfrq, tft3d, 1);
            return tf;
        }

        public void setPoseArrayMsg() {
            List<Pose> pa = new ArrayList<Pose>();
            //get orientation
            Transform3D tfr = new Transform3D();
            this.getRotationTransform(tfr);

            Quat4d ori = new Quat4d();
            Point3d pt = new Point3d();
            for (int i = 0; i < 50; i++) {
                // in robot base frame
                pt.setX(utilsSim.getGaussianNoise(0, this.radius*2));
                pt.setZ(utilsSim.getGaussianNoise(0, this.radius*2));
                tfr.rotY(utilsSim.getGaussianNoise(0, 0.5));
                tfr.get(ori);
                pa.add(utilsSim.getPose(pt, ori));
            }
            poseArray.setPoses(pa);
        }

        public void shutDown() {
//            agentController.stop();
            agentNode.shutDown();
        }

    }

    /**
     * Describe the environement
     */
    static public class MyEnv extends EnvironmentDescription {

        public MyEnv() {
            light1IsOn = true;
            light2IsOn = false;
//            setRobotsPoseRectangle(Parameters.ROBOT_EACH_SIDE);
            setRobotsPoseCircle();
//            setRobotsPoseDense(robotNb,Parameters.FOOTPRINT_RADIUS);
        }

        public void setRobotsPoseDense(int rn,double r){
            double radius=r*1.1;
            float worldsize=(float)(rn*radius*2+2);
            setWorldSize(worldsize);

            Wall w1 = new Wall(new Vector3d(worldsize/2, 0, 0), worldsize, 2, this);
            w1.rotate90(1);
            add(w1);
            Wall w2 = new Wall(new Vector3d(-worldsize/2, 0, 0), worldsize, 2, this);
            w2.rotate90(1);
            add(w2);
            Wall w3 = new Wall(new Vector3d(0, 0, worldsize/2), worldsize, 2, this);
            add(w3);
            Wall w4 = new Wall(new Vector3d(0, 0, -worldsize/2), worldsize, 2, this);
            add(w4);

            robots.clear();
            double ori;
            int idx=0;
            for (int i = 0; i < rn; i++) {
                for (int j = 0; j < rn; j++) {
                    double x=rn*radius-i*2*radius-radius;
                    double z=rn*radius-j*2*radius-radius;
                    // shift position so that no robot is placed on the origin
                    if (rn%2!=0){
                        x-=radius/4;
                        z-=radius/4;
                    }
                    Vector3d pose = new Vector3d(x, 0, z);

                    if(x!=0){
                        if (x>0){
                            ori=Math.atan(-z/x);
                        }else {
                            ori=Math.atan(-z/x)+Math.PI;
                        }
                    }else {
                        if (z>0){
                            ori=-Math.PI/2;
                        }else {
                            ori=Math.PI/2;
                        }
                    }
                    Robot robot = new Robot(pose, Math.PI+ori, idx);
                    robots.add(robot);
                    add(robot);
                    idx++;
                }

            }

        }

        public void setRobotsPoseCircle(){
            float worldsize=(float)posRadius*2+4;
            setWorldSize(worldsize);

            Wall w1 = new Wall(new Vector3d(worldsize/2, 0, 0), worldsize, 2, this);
            w1.rotate90(1);
            add(w1);
            Wall w2 = new Wall(new Vector3d(-worldsize/2, 0, 0), worldsize, 2, this);
            w2.rotate90(1);
            add(w2);
            Wall w3 = new Wall(new Vector3d(0, 0, worldsize/2), worldsize, 2, this);
            add(w3);
            Wall w4 = new Wall(new Vector3d(0, 0, -worldsize/2), worldsize, 2, this);
            add(w4);

            robots.clear();
            double step = 2 * Math.PI / robotNb;
            for (int i = 0; i < robotNb; i++) {
                Vector3d pose = new Vector3d(posRadius * Math.cos(i * step), 0, -posRadius * Math.sin(i * step));
                Robot robot = new Robot(pose, Math.PI + i * step, i);
                robots.add(robot);
                add(robot);
            }

        }

        public void setRobotsPoseRectangle(int robotNumberOnEachSide){
            robots.clear();
            double dl=1.5;
            double l=dl*robotNumberOnEachSide;
            double end=-l/2.0+dl/2;


            float worldsize=(float)l+2;
            this.setWorldSize(worldsize);
            Wall w1 = new Wall(new Vector3d(worldsize/2, 0, 0), worldsize, 2, this);
            w1.rotate90(1);
            add(w1);
            Wall w2 = new Wall(new Vector3d(-worldsize/2, 0, 0), worldsize, 2, this);
            w2.rotate90(1);
            add(w2);
            Wall w3 = new Wall(new Vector3d(0, 0, worldsize/2), worldsize, 2, this);
            add(w3);
            Wall w4 = new Wall(new Vector3d(0, 0, -worldsize/2), worldsize, 2, this);
            add(w4);

            int cnt=0;
            for (int i = 0; i < robotNumberOnEachSide; i++) {
                Vector3d pose1=new Vector3d(l/2,0,end+dl*i);
                Robot robot1=new Robot(pose1,Math.PI,cnt++);
                robots.add(robot1);

                Vector3d pose2=new Vector3d(-l/2,0,end+dl*i);
                Robot robot2=new Robot(pose2,0,cnt++);
                robots.add(robot2);

                Vector3d pose3=new Vector3d(end+dl*i,0,-l/2);
                Robot robot3=new Robot(pose3,-Math.PI/2,cnt++);
                robots.add(robot3);

                Vector3d pose4=new Vector3d(end+dl*i,0,l/2);
                Robot robot4=new Robot(pose4,Math.PI/2,cnt++);
                robots.add(robot4);
            }

            for (int i = 0; i < robots.size(); i++) {
                this.add(robots.get(i));
            }

        }
    }

    public static void main(String[] args) {
        // request antialising
        System.setProperty("j3d.implicitAntialiasing", "true");

        Map<String,String> params=getProperties(args);
        if (params!=null){
            if(params.get(ROBOT_NUMBER_KEY)!=null)
                robotNb=Integer.parseInt(params.get(ROBOT_NUMBER_KEY));
            if (params.get(ROBOT_POSE_RADIUS_KEY)!=null)
                posRadius=Double.parseDouble(params.get(ROBOT_POSE_RADIUS_KEY));
            if (params.get(BACKGROUND_MODE_KEY)!=null)
                bgMode=true;
            if (params.get(FILE_NAME_KEY)!=null)
                fname=params.get(FILE_NAME_KEY);
        }
      // create Simbad instance with given environment, can not use background mode
        Simbad frame = new Simbad(new MyEnv(), false);

        // run the simulation
//        runSimulation(frame);
//
//        runMetric(fname);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
//                delayRecorder.close();
//                collisionRecorder.close();

//                getboltdelay.shutdown();
//                writetofile.close();
                for (Robot robot : robots) {
                    System.out.println("Shutting down " + robot.getName());
                    robot.shutDown();
                }
            }
        });
    }
    
//    private static void runSimulation(Simbad frame){
//        JInternalFrame[] frames=frame.getDesktopPane().getAllFrames();
//        // click on run button
//        frames[0].getContentPane().getComponent(1).
//                getAccessibleContext().getAccessibleChild(0).
//                getAccessibleContext().getAccessibleChild(0).
//                getAccessibleContext().getAccessibleAction().doAccessibleAction(0);
//        // view from top
//        frames[0].getContentPane().getComponent(0).
//                getAccessibleContext().getAccessibleChild(0).
//                getAccessibleContext().getAccessibleChild(0).
//                getAccessibleContext().getAccessibleChild(0).
//                getAccessibleContext().getAccessibleAction().doAccessibleAction(0);
//    }

//    public static void runMetric(String fname) {
//        delayRecorder = new DataRecorder(fname+".yaml");
//        delayRecorder.open();
//        collisionRecorder=new DataRecorder(fname+".coll");
//        collisionRecorder.open();
//        computeDelayRecorder=new DataRecorder(fname+".delay");
//        DelayDataReceiver.ComputeDelayReceiver delayReceiver=
//                new DelayDataReceiver.ComputeDelayReceiver(computeDelayRecorder, Constants.remoteRMQUrl);
//    }


    private static Map<String, String> getProperties(String[] args) {
        Map<String, String> conf = new HashMap<String, String>();

        Options options = new Options();
        options.addOption(ROBOT_NUMBER_IPT_NAME, true, "number of robots");
        options.addOption(ROBOT_POSE_RADIUS_IPT_NAME,true,"pose radius for robots");
        options.addOption(FILE_NAME_IPT_NAME,true,"name of file recording data");
        options.addOption(BACKGROUND_MODE_IPT_NAME,false,"run in back groud mode");

        CommandLineParser commandLineParser = new BasicParser();
        try {
            CommandLine cmd = commandLineParser.parse(options, args);
            // robot number is the square of this input
            String n = cmd.getOptionValue(ROBOT_NUMBER_IPT_NAME);
            conf.put(ROBOT_NUMBER_KEY, n);
            String r=cmd.getOptionValue(ROBOT_POSE_RADIUS_IPT_NAME);
            conf.put(ROBOT_POSE_RADIUS_KEY,r);
            String f=cmd.getOptionValue(FILE_NAME_IPT_NAME);
            conf.put(FILE_NAME_KEY,f);
            if (cmd.hasOption(BACKGROUND_MODE_IPT_NAME)) {
                conf.put(BACKGROUND_MODE_KEY, "true");
            }
            return conf;
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("simulator", options);
        }
        return null;
    }
}
