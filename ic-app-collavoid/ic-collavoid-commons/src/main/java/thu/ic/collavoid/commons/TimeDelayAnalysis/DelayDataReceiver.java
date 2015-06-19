package thu.ic.collavoid.commons.TimeDelayAnalysis;

import thu.ic.collavoid.commons.rabbitmq.Message;
import thu.ic.collavoid.commons.rabbitmq.MessageHandler;
import thu.ic.collavoid.commons.rabbitmq.MessagingConstants;
import thu.ic.collavoid.commons.rabbitmq.RabbitMQReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DelayDataReceiver {

    public static class ComputeDelayReceiver{
        private RabbitMQReceiver computeDelayreceiver;
        private ComputeDelayHandler computeDelayHandler=new ComputeDelayHandler();
        private DataRecorder computeDelayRecorder;
        private String rmqurl=Constants.localRMQUrl;
        public ComputeDelayReceiver(DataRecorder dataRecorder,String url){
            computeDelayRecorder=dataRecorder;
            if (url!=null){
                rmqurl=url;
            }
            try {
                computeDelayreceiver=new RabbitMQReceiver(rmqurl,Constants.exchangeName,true);
                computeDelayreceiver.listen(computeDelayHandler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public class ComputeDelayHandler implements MessageHandler {
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
                for (Map.Entry<String,Object> e:delayMsg.entrySet()){
                    data.clear();
                    data.add(e.getKey());
                    data.add(e.getValue().toString());
                    computeDelayRecorder.append(data);
                }
            }
        }


    }

}
