package rabbitmq;

import thu.ic.collavoid.commons.rmqmsg.Constant_msg;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by hjh on 12/17/14.
 */
public class Talker {
    private final static String EXCHANGE_NAME = "robot0rmq";

    public static void main(String[] argv)
            throws java.io.IOException, InterruptedException {

        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(Constant_msg.RMQ_URL_REMOTE);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
//        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // not needed, only need routing key
        String queueName = channel.queueDeclare().getQueue();
        channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);


        int i = 0;
        while (i++ < 1000) {
            String message = ""+System.currentTimeMillis();
            channel.basicPublish(EXCHANGE_NAME, "odometryRoutingKey", null, message.getBytes());
            Thread.sleep(1000);
        }

        channel.close();
        connection.close();

    }

}
