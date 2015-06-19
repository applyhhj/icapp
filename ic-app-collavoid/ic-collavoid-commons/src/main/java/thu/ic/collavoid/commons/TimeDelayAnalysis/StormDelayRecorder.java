package thu.ic.collavoid.commons.TimeDelayAnalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class StormDelayRecorder {
    private HttpClient client;
    private HttpGet request;
    HttpResponse response;

    public StormDelayRecorder(String topologyName, String compId) {
        client = new DefaultHttpClient();
        request = new HttpGet("http://nz:8080/api/v1/topology/" + getTopologyID(topologyName) + "/component/" + compId);
    }

    private String getTopologyID(String topName) {
        String res = "";
        request = new HttpGet("http://nz:8080/api/v1/topology/summary");
        try {
            response = client.execute(request);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            JSONObject obj = new JSONObject(rd.readLine());
            JSONArray array = obj.getJSONArray("topologies");
            for (int i = 0; i < array.length(); i++) {
                JSONObject entry = array.getJSONObject(i);
                if (entry.getString("name").equals(topName)) {
                    res = entry.getString("id");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public String get() {
        String res = "";
        try {
            response = client.execute(request);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            JSONObject obj = new JSONObject(rd.readLine());
            res = obj.getJSONArray("boltStats").getJSONObject(0).getString("processLatency");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

}
