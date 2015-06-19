import thu.ic.collavoid.commons.planners.Vector2;
import thu.ic.collavoid.commons.storm.BoltContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

/**
 * Created by hjh on 12/18/14.
 */
public class testSerialize {
    private static class OwnFootprintContext implements BoltContext {
        private List<Vector2> ownFootprint = null;

        @Override
        public void reset() {
            ownFootprint = null;
        }

        public void setOwnFootprint(List<Vector2> ownFootprint) {
            this.ownFootprint = ownFootprint;
        }

        public List<Vector2> getOwnFootprint() {
            return ownFootprint;
        }
    }

    public static void main(String[] args) throws ClientProtocolException, IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://nz:8080/api/v1/topology/ca-1-1425061694/component/voLinesCompute");
        HttpResponse response = client.execute(request);
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = rd.readLine();
        System.out.println(line);
        JSONObject obj = new JSONObject(line);
        String n = obj.getJSONArray("boltStats").getJSONObject(0).getString("processLatency");
        System.out.println(n);
    }

    public static void testFinal(final Vector2 vector2) {
        vector2.setX(vector2.getX() + 2);

    }
    
    
}
