package thu.ic.collavoid.commons.TimeDelayAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by hjh on 2/1/15.
 */
public class DelayRecorder {
    private Logger logger = LoggerFactory.getLogger(TimeDelayRecorder.class);
    private String path;
    private String fileName;
    private String component;
    private FileWriter fileWriter = null;
    private File file;
    private long initTime = 0;

    public DelayRecorder(String type, String parameter, String component) {
        if (type.equals(Constants.COMPUTATION_DELAY))
            path = Constants.basedir + type + "/";
        else
            path = Constants.basedir + type + "/" + parameter + "/";

        fileName = path + component;
        this.component = component;
        open();
    }

    private void open() {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            file = new File(fileName);
            fileWriter = new FileWriter(file);
            synchronized (file) {
                fileWriter.write("Time(s)," + component + "_delay(ms)" + "\n");
                fileWriter.flush();
            }
            fileWriter = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void append(long value) {
        synchronized (file) {
            try {
                fileWriter.write(System.nanoTime() + "," + value + "\n");
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
