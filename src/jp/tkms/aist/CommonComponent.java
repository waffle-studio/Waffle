package jp.tkms.aist;

import java.io.*;
import java.util.ArrayList;

public class CommonComponent implements Serializable {
    private PollingMonitor pollingMonitor;
    private ArrayList<ExpSet> expSetList;

    public CommonComponent(){
        pollingMonitor = null;
        expSetList = new ArrayList<>();
    }

    public ArrayList<ExpSet> addExpSet(ExpSet expSet){
        expSetList.add(expSet);
        return expSetList;
    }

    public PollingMonitor getPollingMonitor() {
        if (pollingMonitor == null) {
            pollingMonitor = new PollingMonitor();
        }
        return pollingMonitor;
    }

    public ArrayList<ExpSet> getExpSetList() {
        return expSetList;
    }

    public void save(String fileName) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(this);
        objectOutputStream.flush();
        objectOutputStream.close();
    }

    public static CommonComponent load(String fileName) {
        CommonComponent data = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(fileName);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            data = (CommonComponent)objectInputStream.readObject();
            objectInputStream.close();
        } catch (Exception e) {
            data = new CommonComponent();
        }
        return data;
    }
}
