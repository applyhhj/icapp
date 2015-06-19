package thu.ic.collavoid.storm;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.generated.KillOptions;
import backtype.storm.generated.Nimbus;
import backtype.storm.generated.NotAliveException;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.NimbusClient;
import backtype.storm.utils.Utils;
import thu.ic.collavoid.commons.ic.ModStreamGrouping;
import thu.ic.collavoid.commons.storm.Constant_storm;
import org.apache.thrift7.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thu.instcloud.storm.api.bolts.ICRMQBolt;
import thu.instcloud.storm.api.core.ICStormComponentBuilder;
import thu.instcloud.storm.api.core.ICStormComponents;
import thu.instcloud.storm.api.spouts.ICRMQSpout;

import java.util.HashMap;
import java.util.Map;

public class ICTopologyBuilder {
    private Logger logger = LoggerFactory.getLogger(ICTopologyBuilder.class);
    private LocalCluster localCluster = null;
    private Config config;
    private Map<String, ICRMQSpout> spoutMap = new HashMap<>();
    private Map<String, ICRMQBolt> boltMap = new HashMap<>();
    private StormTopology stormTopology;
    private String toplogyName;
    private TopologyBuilder builder = new TopologyBuilder();
    private int paraCompute=3;
    private int paraState=1;


    public ICTopologyBuilder(final Config config) {
        this(null, config, null);
    }

    public ICTopologyBuilder(final LocalCluster localCluster, Config config, String toplogyName) {
        if (localCluster != null) {
            this.localCluster = localCluster;
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        localCluster.shutdown();
                    } catch (Exception e) {
                    }
                }
            });
        }
        this.config = config;
        this.toplogyName = toplogyName;
        ICStormComponentBuilder icStormComponentBuilder=new ICStormComponentBuilder();
        ICStormComponents components=icStormComponentBuilder.build();
        spoutMap = components.getSpouts();
        boltMap = components.getBolts();
    }

    public void submit() {
        if (localCluster == null) {
            logger.error("Not in local mode, localCluster not received!!");
            return;
        }

        localCluster.submitTopology(toplogyName, config, stormTopology);
    }

    public StormTopology getStormTopology(int paraCompute,int paraState) {
        this.paraCompute=paraCompute;
        this.paraState=paraState;
        buildTopology();
        return stormTopology;
    }

    public void shutdown() {
        Map conf = Utils.readStormConfig();
        Nimbus.Client client = NimbusClient.getConfiguredClient(conf).getClient();
        KillOptions killOpts = new KillOptions();
        try {
            client.killTopologyWithOpts(toplogyName, killOpts); //provide topology name
        } catch (NotAliveException e) {
            e.printStackTrace();
        } catch (TException e) {
            e.printStackTrace();
        }

    }


    public void buildTopology() {
        //spout
        for (Map.Entry<String, ICRMQSpout> e : spoutMap.entrySet()) {
            builder.setSpout(e.getKey(), e.getValue(), 1);
        }
        builder.setSpout(Constant_storm.Components.TIMER_SPOUT_COMPONENT, new TimerSpout());

        //bolt
        // manually ack tuple
        builder.setBolt(Constant_storm.Components.GLOBAL_PLANNER_COMPONENT,new GlobalPlannerBolt(),1)
                .allGrouping(Constant_storm.Components.TIMER_SPOUT_COMPONENT,
                        Constant_storm.Streams.TIMEOUT_STREAM)
                .customGrouping(Constant_storm.Components.COMMAND_SPOUT_COMPONENT,
                        new ModStreamGrouping())
                .customGrouping(Constant_storm.Components.AGENT_STATE_COMPONENT,
                        Constant_storm.Streams.ACK_STREAM,
                        new ModStreamGrouping())
                .customGrouping(Constant_storm.Components.DISPATCHER_COMPONENT,
                        Constant_storm.Streams.ACK_STREAM,
                        new ModStreamGrouping())
                .customGrouping(Constant_storm.Components.VELOCITY_COMPUTE_COMPONENT,
                        Constant_storm.Streams.ACK_STREAM,
                        new ModStreamGrouping());

        builder.setBolt(Constant_storm.Components.DISPATCHER_COMPONENT,new DispatcherBolt(),1)
                .allGrouping(Constant_storm.Components.TIMER_SPOUT_COMPONENT,
                        Constant_storm.Streams.CTLPUB_TIMEER_STREAM)
                .customGrouping(Constant_storm.Components.GLOBAL_PLANNER_COMPONENT,
                        Constant_storm.Streams.CTL_PUB_TIME_STREAM,
                        new ModStreamGrouping())
                .customGrouping(Constant_storm.Components.AGENT_STATE_COMPONENT,
                        Constant_storm.Streams.PUBLISHME_STREAM,
                        new ModStreamGrouping())
                .shuffleGrouping(Constant_storm.Components.AGENT_STATE_COMPONENT,
                        Constant_storm.Streams.ROBOT_TIMEOUT_STREAM)
                .customGrouping(Constant_storm.Components.VELOCITY_COMPUTE_COMPONENT,
                        Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM,
                        new ModStreamGrouping());

        builder.setBolt(Constant_storm.Components.VELOCITY_COMPUTE_COMPONENT,new VelocityComputeBolt(),paraCompute)
                .customGrouping(Constant_storm.Components.AGENT_STATE_COMPONENT,
                        Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM,
                        new ModStreamGrouping())
                .shuffleGrouping(Constant_storm.Components.AGENT_STATE_COMPONENT,
                        Constant_storm.Streams.ROBOT_TIMEOUT_STREAM)
                .customGrouping(Constant_storm.Components.GLOBAL_PLANNER_COMPONENT,
                        Constant_storm.Streams.AGENT_STREAM,
                        new ModStreamGrouping());

        builder.setBolt(Constant_storm.Components.AGENT_STATE_COMPONENT,new AgentStateBolt(),paraState)
                .allGrouping(Constant_storm.Components.POSE_SHARE_COMPONENT)
                .customGrouping(Constant_storm.Components.ODOMETRY_SPOUT_COMPONENT,
                        new ModStreamGrouping())
                .customGrouping(Constant_storm.Components.POSE_ARRAY_COMPONENT,
                        new ModStreamGrouping())
                .customGrouping(Constant_storm.Components.SCAN_COMPONENT,
                        new ModStreamGrouping())
                .customGrouping(Constant_storm.Components.DISPATCHER_COMPONENT,
                        Constant_storm.Streams.CALCULATE_VELOCITY_CMD_STREAM,
                        new ModStreamGrouping())
                .customGrouping(Constant_storm.Components.DISPATCHER_COMPONENT,
                        Constant_storm.Streams.PUBLISHME_STREAM,
                        new ModStreamGrouping())
                .customGrouping(Constant_storm.Components.GLOBAL_PLANNER_COMPONENT,
                        Constant_storm.Streams.POSE_SHARE_MSG_STREAM,
                        new ModStreamGrouping());

        // ic component
        builder.setBolt(Constant_storm.Components.VELOCITY_COMMAND_PUBLISHER_COMPONENT,
                boltMap.get(Constant_storm.Components.VELOCITY_COMMAND_PUBLISHER_COMPONENT), 1)
                .shuffleGrouping(Constant_storm.Components.VELOCITY_COMPUTE_COMPONENT,
                        Constant_storm.Streams.VELOCITY_COMMAND_STREAM);

        builder.setBolt(Constant_storm.Components.POSE_SHARE_PUB_COMPONENT,
                boltMap.get(Constant_storm.Components.POSE_SHARE_PUB_COMPONENT), 1)
                .shuffleGrouping(Constant_storm.Components.AGENT_STATE_COMPONENT,
                        Constant_storm.Streams.PUBLISHME_PUB_STREAM);

        stormTopology = builder.createTopology();
    }
}
