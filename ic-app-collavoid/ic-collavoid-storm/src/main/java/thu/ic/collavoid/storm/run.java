package thu.ic.collavoid.storm;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import thu.ic.collavoid.commons.storm.Methods_storm;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class run {

    private static Logger LOG = LoggerFactory.getLogger(run.class);
    private static String PARALLEL_COMPUTE="pc";
    private static String PARALLEL_STATE="ps";

    public static void main(String[] args) throws Exception {

        Config conf = new Config();
        conf.setDebug(false);
        conf.put(Config.TOPOLOGY_ACKER_EXECUTORS, 0);// very important

        // add the serializers
        Methods_storm.addSerializers(conf);
    

        // we are going to deploy on a real cluster
        if (args != null && args.length > 0) {
            int paraCompute=Integer.parseInt(getProperties(args).get(PARALLEL_COMPUTE));
            int paraState=Integer.parseInt(getProperties(args).get(PARALLEL_STATE));
            conf.setNumWorkers(20);
            final ICTopologyBuilder topology = new ICTopologyBuilder(conf);
            StormSubmitter.submitTopology(args[0], conf, topology.getStormTopology(paraCompute,paraState));
            LOG.info("\n********************Planner started. Running on the cluster!!***********************");
        } else {
            // deploy on a local cluster
            conf.setMaxTaskParallelism(3);
            final LocalCluster cluster = new LocalCluster("localhost", new Long(2181));
            final ICTopologyBuilder topology = new ICTopologyBuilder(cluster, conf, "ca_planner");
            topology.buildTopology();
            topology.submit();

            Thread.sleep(1000000);
            LOG.info("Stopping topology.....................................!!");
            cluster.killTopology("default");
            cluster.shutdown();
        }
    }


    private static Map<String, String> getProperties(String[] args) {
        Map<String, String> conf = new HashMap<String, String>();

        Options options = new Options();
        options.addOption(PARALLEL_COMPUTE, true, "Set computation bolt parallelism!");
        options.addOption(PARALLEL_STATE,true,"Set agent state bolt parallelism!");

        CommandLineParser commandLineParser = new BasicParser();
        try {
            CommandLine cmd = commandLineParser.parse(options, args);
            String p = cmd.getOptionValue(PARALLEL_COMPUTE);
            conf.put(PARALLEL_COMPUTE, p);
            p=cmd.getOptionValue(PARALLEL_STATE);
            conf.put(PARALLEL_STATE,p);
            return conf;
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("topology", options);
        }
        return null;
    }



}
