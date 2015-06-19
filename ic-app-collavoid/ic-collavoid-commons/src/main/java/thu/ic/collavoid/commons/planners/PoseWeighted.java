package thu.ic.collavoid.commons.planners;

import thu.ic.collavoid.commons.rmqmsg.Pose_;

import java.io.Serializable;

public class PoseWeighted implements Serializable {
    private double w;
    private Pose_ Pose;

    public PoseWeighted() {
        Pose = new Pose_();
    }

    public void setW(double w){
        this.w=w;
    }

    public void setPose(Pose_ pose) {
        Pose = pose;
    }

    public double getW(){
        return this.w;
    }

    public Pose_ getPose() {
        return Pose;
    }
}
