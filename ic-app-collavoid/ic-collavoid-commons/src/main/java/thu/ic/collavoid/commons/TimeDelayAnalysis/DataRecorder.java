package thu.ic.collavoid.commons.TimeDelayAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class DataRecorder {
    private Logger logger = LoggerFactory.getLogger(TimeDelayRecorder.class);
    private String basedir ="/home/hjh/data/fullCompDelay/";
    private String filePath="";
    private String fileName="";
    private FileWriter fileWriter = null;
    private File file;
    private boolean open;
    private long initTime = 0;

    public DataRecorder(String fname){
        fileName=fname;
        open();
    }

    public DataRecorder(String subdir, String fname) {
        basedir = basedir +subdir+"/";
        fileName=fname;
        open();
    }

    public void setBasedir(String basepath){
        basedir =basepath;
        open();
    }


    public void open() {
        if (!filePath.equals(basedir+fileName)) {
            filePath = basedir + fileName;
            close();
        }
        if (open)
            return;
        File dir = new File(basedir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            file = new File(filePath);
            fileWriter = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        open=true;
    }

    public void append(List<String> valueList) {
        String content = valueList.get(0);
        for (int i = 1; i < valueList.size(); i++) {
            content = content + "," + valueList.get(i);
        }
        synchronized (file) {
            try {
                fileWriter.write(content + "\n");
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (fileWriter!=null)
        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        open=false;
    }
}
