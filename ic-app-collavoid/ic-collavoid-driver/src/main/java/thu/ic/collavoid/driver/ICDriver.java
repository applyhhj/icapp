package thu.ic.collavoid.driver;

import thu.ic.collavoid.commons.storm.Constant_storm;
import thu.instcloud.client.core.*;
import thu.instcloud.client.utils.ICSessionTask;
import thu.instcloud.common.server.thrift.dtype.TDuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hjh on 6/14/15.
 */
public class ICDriver {
    private static int robotNumber;
    private static TDuration duration;
    public static String robotIp;
    public static String rosMasterIp;

    public static void main(String[] args) throws InterruptedException{
        robotIp="localhost";
//        rosMasterIp="http://localhost:11311";
        rosMasterIp="http://hjh-lab:11311/";
        duration=getDuration();
        robotNumber=4;
        ICClientForSession client=new ICClientForSession();

        for (int i = 0; i < robotNumber; i++) {    
            ICSession session=client.newICSession();
            session.setDurationForAll(duration);
            final RobotContext context=new RobotContext(i,robotIp,rosMasterIp);
            context.setProduces(getStreamProducers(session));
            ICServiceModule planner=session.newICServiceModule("ca_planner_m", "ca_planner");
            
            context.getProduces().get(Constant_storm.IC.channels.ODOMETRY_CHANNEL)
                    .consumeService("ca_planner_m",Constant_storm.Components.ODOMETRY_SPOUT_COMPONENT);
            context.getProduces().get(Constant_storm.IC.channels.SCAN_CHANNEL)
                    .consumeService("ca_planner_m",Constant_storm.Components.SCAN_COMPONENT);
            context.getProduces().get(Constant_storm.IC.channels.POSE_ARRAY_CHANNEL)
                    .consumeService("ca_planner_m",Constant_storm.Components.POSE_ARRAY_COMPONENT);
            context.getProduces().get(Constant_storm.IC.channels.COMMAND_CHANNEL)
                    .consumeService("ca_planner_m",Constant_storm.Components.COMMAND_SPOUT_COMPONENT);

            StreamConsumer velCmdRecv=session.newSink("vel_cmd_recv");
            velCmdRecv.setMessageHandler(new VelocityCommandHandler(context.getVelocityQueue()));
            
            planner.consumeService(Constant_storm.Components.VELOCITY_COMMAND_PUBLISHER_COMPONENT,"vel_cmd_recv");

            session.setICSessionTask(new ICSessionTask() {
                RobotController controller = new RobotController(context);

                @Override
                public void start() {
                    controller.start();
                }

                @Override
                public void stop() {
                    controller.stop();
                }
            });
            
            session.open();            
        }

        System.out.println("Application started!!");
        
        while (client.hasSessionRunning()){
            Thread.sleep(1000);
        }
        client.close();
    }
    
    private static TDuration getDuration(){
        long start=System.currentTimeMillis()+8000;
        long end=System.currentTimeMillis()+500000;
        TDuration ret=new TDuration();
        ret.setDstart(start);
        ret.setDend(end);
        return ret;
    }
    
    private static Map<String,StreamProducer> getStreamProducers(ICSession session){
        Map<String,StreamProducer> producerMap=new HashMap<>();
        producerMap.put(Constant_storm.IC.channels.ODOMETRY_CHANNEL,session.newHeader(
                Constant_storm.IC.channels.ODOMETRY_CHANNEL));
        producerMap.put(Constant_storm.IC.channels.SCAN_CHANNEL,session.newHeader(
                Constant_storm.IC.channels.SCAN_CHANNEL));
        producerMap.put(Constant_storm.IC.channels.POSE_ARRAY_CHANNEL,session.newHeader(
                Constant_storm.IC.channels.POSE_ARRAY_CHANNEL));
        producerMap.put(Constant_storm.IC.channels.COMMAND_CHANNEL,session.newHeader(
                Constant_storm.IC.channels.COMMAND_CHANNEL));
        return producerMap;
        
    }
}
