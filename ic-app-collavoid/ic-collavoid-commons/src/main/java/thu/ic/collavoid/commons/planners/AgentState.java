package thu.ic.collavoid.commons.planners;

import thu.ic.collavoid.commons.rmqmsg.Odometry_;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//for simple topology

public class AgentState implements Serializable {
    public String id;
    public Odometry_ odometry_=null;
    public List<Neighbor> neighbors=new ArrayList<>();
    public List<Obstacle> obstacles=new ArrayList<>();
    public List<Vector2> minkowskiFootprint=new ArrayList<>();

    public AgentState(){

    }

    public AgentState(String id){
        this.id=id;
    }
}