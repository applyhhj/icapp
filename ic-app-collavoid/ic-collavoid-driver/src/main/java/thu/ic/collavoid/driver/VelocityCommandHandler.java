package thu.ic.collavoid.driver;

import thu.ic.collavoid.commons.rmqmsg.Methods_RMQ;
import thu.ic.collavoid.commons.rmqmsg.Twist_;
import com.esotericsoftware.kryo.Kryo;
import io.latent.storm.rabbitmq.Message;
import thu.instcloud.client.utils.MessageHandler;

import java.util.concurrent.BlockingQueue;

/**
 * Created by hjh on 6/14/15.
 */
public class VelocityCommandHandler implements MessageHandler {
    private BlockingQueue<Twist_> velQueue;
    private Kryo kryo;
    
    public VelocityCommandHandler(BlockingQueue<Twist_> velQueue){
        this.velQueue=velQueue;
        this.kryo= Methods_RMQ.getKryo();
    }

    @Override
    public void onNewMessage(Message.DeliveredMessage deliveredMessage) {
        velQueue.offer((Twist_) Methods_RMQ.deSerialize(kryo, deliveredMessage.getBody()));
    }
}
