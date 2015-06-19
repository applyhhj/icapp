import thu.ic.collavoid.commons.planners.Parameters;
import simbad.gui.Simbad;
import simbad.sim.*;

import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.List;

public class simupose {
    static double r= Parameters.FOOTPRINT_RADIUS;
    static List<Robot> robots=new ArrayList<>();

    /** Describe the robot */
    static public class Robot extends Agent {

        private double orientation;

        public Robot(Vector3d position,double ori, int name) {
            super(position, name+"");
            orientation=ori;
            this.radius=(float)r;

        }

        /** This method is called by the simulator engine on reset. */
        public void initBehavior() {
            // nothing particular in this case
            this.rotateY(orientation);
        }

        /** This method is call cyclically (20 times per second)  by the simulator engine. */
        public void performBehavior() {

            if (this.anOtherAgentIsVeryNear())
                System.out.println("hit");

        }
    }

    /** Describe the environement */
    static public class MyEnv extends EnvironmentDescription {
        public MyEnv(int rn) {
            light1IsOn = true;
            light2IsOn = false;
            setRobotsPoseDense(rn,r);
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
    }

    public static void main(String[] args) {
        // request antialising
        System.setProperty("j3d.implicitAntialiasing", "true");
        // create Simbad instance with given environment
        Simbad frame = new Simbad(new MyEnv(10), false);

    }

}
