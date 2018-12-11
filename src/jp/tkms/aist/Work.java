package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.io.*;
import java.util.HashMap;
import java.util.UUID;

public class Work implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private File workBase;

    private HashMap<UUID, ExpSet> expSetMap;

    @Override
    public String toString() {
        return name + "\n" +
                "# Set(" + expSetMap.size() + ")";
    }

    public Work(String name) {
        this.name = name;
        workBase = new File(Config.LOCAL_WORKBASE_DIR + "/" + name);

        expSetMap = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public ExpSet newExpSet(PollingMonitor pollingMonitor, String seriesName, String preScript, String postScript, String exec) {
        ExpSet expSet = new ExpSet(pollingMonitor, this, getName() + "_" + seriesName, preScript, postScript, exec);
        expSetMap.put(expSet.getUuid(), expSet);
        return expSet;
    }

    public HashMap<UUID, ExpSet> getExpSetMap() {
        return expSetMap;
    }

    public ExpSet getExpSet(String id) {
        return expSetMap.get(UUID.fromString(id));
    }

    public void setup() {
        workBase.mkdirs();
        File localBaseRemote = new File(Config.LOCAL_WORKBASE_DIR + "/" + name + "/REMOTE");
        localBaseRemote.mkdirs();
        String remoteBase = Config.REMOTE_WORKBASE_DIR + "/" + name;
        try {
            SshSession sshSession = new AbciSshSession();
            sshSession.mkdir(getRemoteWorkBase(), Config.REMOTE_WORKBASE_DIR);
            sshSession.scp(localBaseRemote, remoteBase, Config.REMOTE_WORKBASE_DIR);
            sshSession.disconnect();
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    public String getRemoteWorkBase() {
        return Config.REMOTE_WORKBASE_DIR + "/" + name;
    }

    public String getTextFile(String path) {
        File textFile = new File(workBase.getPath() + "/" + path);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile)));
            StringBuffer stringBuffer = new StringBuffer();
            int c;
            while ((c = reader.read()) != -1) {
                stringBuffer.append((char) c);
            }
            return stringBuffer.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public void save() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(getWorkDatFileName(getName()));
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.flush();
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Work load(String name) {
        Work work = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(getWorkDatFileName(name));
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            work = (Work) objectInputStream.readObject();
            objectInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            work = null;
        }
        return work;
    }

    public static String getWorkDatFileName(String name) {
        return Config.LOCAL_WORKBASE_DIR + "/" + name + "/work.dat";
    }
}
