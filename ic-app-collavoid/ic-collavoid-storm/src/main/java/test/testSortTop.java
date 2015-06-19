package test;

import thu.ic.collavoid.commons.planners.Agent;

import java.util.HashMap;
import java.util.Map;

public class testSortTop {
    static Map<String, Agent> testmap=new HashMap<>();
    public static void main(String[] args){
        String tst="a,b,c,1";
        long t=System.currentTimeMillis();
        System.out.println(t);
        int pos=tst.lastIndexOf(",");
        String res=tst.substring(0,pos+1)+t+","+tst.substring(pos+1,tst.length());
        System.out.println(res);

//        for (int i = 0; i < 10; i++) {
//            int n=i*i;
//            System.out.println((int)Math.sqrt(n));
//        }
//        while (true){
//            System.out.println("exhaust cpu!!");
//        }
    }
}
