package test;

import thu.ic.collavoid.commons.TimeDelayAnalysis.Constants;
import thu.ic.collavoid.commons.TimeDelayAnalysis.DataRecorder;
import thu.ic.collavoid.commons.rabbitmq.Message;
import thu.ic.collavoid.commons.rabbitmq.MessageHandler;
import thu.ic.collavoid.commons.rabbitmq.MessagingConstants;
import thu.ic.collavoid.commons.rabbitmq.RabbitMQReceiver;
import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class receiveDelayData {
    static private RabbitMQReceiver communicationDelayreceiver;

    static private CommnuicationDelayHandler commnuicationDelayHandler=new CommnuicationDelayHandler();
    static private DataRecorder communicationDelayRecorder;

    static String FNAME_CMD="f";
    static String fname;

    public static void main(String[] args) throws Exception{
        fname=getProperties(args).get(FNAME_CMD);
        communicationDelayRecorder=new DataRecorder(fname);
        communicationDelayreceiver=new RabbitMQReceiver(Constants.localRMQUrl,Constants.exchangeName,true);
        communicationDelayreceiver.listen(commnuicationDelayHandler);
    }


    public static class CommnuicationDelayHandler implements MessageHandler{
        @Override
        public Map<String, String> getProperties() {
            Map<String,String> prop=new HashMap<>();
            prop.put(MessagingConstants.RABBIT_ROUTING_KEY,Constants.routingKey);
            return prop;
        }

        @Override
        public void onMessage(Message message) {
            Map<String,Object> delayMsg=message.getProperties();
            List<String> data=new ArrayList<>();
            for(Map.Entry<String,Object> e:delayMsg.entrySet()){
                data.add(e.getKey()+","+e.getValue().toString());
            }
            communicationDelayRecorder.append(data);
        }
    }

    private static Map<String, String> getProperties(String[] args) {
        Map<String, String> conf = new HashMap<String, String>();

        Options options = new Options();
        options.addOption(FNAME_CMD, true, "Set file name!");
        CommandLineParser commandLineParser = new BasicParser();
        try {
            CommandLine cmd = commandLineParser.parse(options, args);
            String p = cmd.getOptionValue(FNAME_CMD);
            conf.put(FNAME_CMD, p);
            return conf;
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("topology", options);
        }
        return null;
    }


}
