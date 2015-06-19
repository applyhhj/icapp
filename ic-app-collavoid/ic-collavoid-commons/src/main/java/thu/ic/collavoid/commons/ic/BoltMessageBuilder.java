package thu.ic.collavoid.commons.ic;

import thu.instcloud.storm.api.bolts.ICBoltScheme;

public class BoltMessageBuilder extends ICBoltScheme {

    @Override
    // serialize from storm,
    public byte[] serialize(Object o) {
        return (byte[])o;
    }
}
