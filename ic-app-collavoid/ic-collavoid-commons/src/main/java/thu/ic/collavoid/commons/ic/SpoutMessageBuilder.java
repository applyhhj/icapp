package thu.ic.collavoid.commons.ic;

import backtype.storm.task.TopologyContext;
import thu.ic.collavoid.commons.rmqmsg.Methods_RMQ;
import com.esotericsoftware.kryo.Kryo;
import thu.instcloud.storm.api.spouts.ICSpoutScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpoutMessageBuilder extends ICSpoutScheme {
    private Kryo kryo;

    @Override
    public void open(Map map, TopologyContext topologyContext) {
        super.open(map, topologyContext);
        kryo = Methods_RMQ.getKryo();
    }

    @Override
    public List<Object> deserialize(byte[] bytes) {
        Object obj=Methods_RMQ.deSerialize(kryo, bytes);
        List<Object> ret=new ArrayList<>();
        ret.add(obj);
        return ret;
    }

}
