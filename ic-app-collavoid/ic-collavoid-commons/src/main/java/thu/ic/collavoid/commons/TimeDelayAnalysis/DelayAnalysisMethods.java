package thu.ic.collavoid.commons.TimeDelayAnalysis;

import thu.ic.collavoid.commons.planners.Agent;

/**
 * Created by hjh on 3/4/15.
 */
public class DelayAnalysisMethods {
    
    public static void delayStart(Agent agent, String id){
        TimeDelay delay=new TimeDelay(id);
        delay.start= System.currentTimeMillis();
        agent.delays.add(delay);
    }
    
    public static void delayEnd(Agent agent){
        agent.delays.get(agent.delays.size()-1).end=System.currentTimeMillis();
        
    }
}
