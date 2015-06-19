import cgl.iotcloud.core.Utils;

import java.util.Map;

public class tstTf {
    public static void main(String[] args) {
        Map<String, String> params = Utils.findAndReadConfigFile("test.yaml");
        System.out.println(params);
//        double thta=Methods_Planners.getYaw(new Vector4d_(0.0, 0.0, 0.9945218953682738, 0.10452846326765573));
//        System.out.println(thta/Math.PI*180);
//        thta=Methods_Planners.getYaw(new Vector4d_(0.0, 0.0, 0.9781476007338052, 0.20791169081776145));
//        System.out.println(thta/Math.PI*180);
//        thta=3.0784841728082313;
//        System.out.println(thta/Math.PI*180);
//        Vector2 vector1=new Vector2(0.9253833380436194,-0.0014032284140929009);
//        Vector2 vector2=new Vector2(0.511397799896116,-0.023675879636291888);
//        Vector2 sub=Vector2.minus(vector2,vector1);
//        System.out.println(Math.atan(sub.getY()/sub.getX()));
    }
}
