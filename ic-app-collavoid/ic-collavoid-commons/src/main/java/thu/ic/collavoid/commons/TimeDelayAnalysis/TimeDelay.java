package thu.ic.collavoid.commons.TimeDelayAnalysis;

import java.io.Serializable;

/**
 * Created by hjh on 3/4/15.
 */
public class TimeDelay implements Serializable{
    public String component;
    public long start;
    public long end;
    
    public TimeDelay(){
        
        
    }
    
    public TimeDelay(String id){
        component=id;
        
    }
}
