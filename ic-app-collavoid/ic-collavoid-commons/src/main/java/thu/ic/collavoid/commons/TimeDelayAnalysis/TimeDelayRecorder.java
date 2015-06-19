package thu.ic.collavoid.commons.TimeDelayAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TimeDelayRecorder {
    private Logger logger = LoggerFactory.getLogger(TimeDelayRecorder.class);
    private String path;
    private String fileName;
    private String component;
    private FileWriter fw = null;
    private boolean autoClose=false;

    public TimeDelayRecorder(String type, String parameter, String component) {
        if (type.equals(Constants.COMPUTATION_DELAY))
            path = Constants.basedir + type + "/";
        else
            path = Constants.basedir + type + "/" + parameter + "/";

        fileName = path + component;
        this.component = component;
    }

    public void open(boolean autoClose_) {
        this.autoClose = autoClose_;
        open();
    }

    public void open() {

        if (fw != null) {
            logger.warn("Already opened the file!!");
            return;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            File file = new File(fileName);
            fw = new FileWriter(fileName);
            synchronized (file) {
                // write the title
                fw.write("robotID"+","+Constants.TIME_TAG + "," + component + "\n");
                fw.flush();
                fw.close();
            }
            if (!autoClose)
                fw = new FileWriter(fileName, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void append(String robotName, long timeTag, long time) {
        String content = new String(robotName + "," + timeTag + "," + time);
        writeContentToFile(content);
    }

    public void append(String robotName, String timeTag, String time) {
        String content = new String(robotName + "," + timeTag + "," + time);
        writeContentToFile(content);
    }

    public void append(List<String> valueList) {
        String content = valueList.get(0);
        for (int i = 1; i < valueList.size(); i++) {
            content = content + "," + valueList.get(i);
        }
        writeContentToFile(content);
    }

    private synchronized void writeContentToFile(String content) {
        try {
            if (autoClose) {
                fw = new FileWriter(fileName, true);
            }

            fw.write(content + "\n");
            fw.flush();
            if (autoClose)
                fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void close() {
        if (!autoClose) {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
