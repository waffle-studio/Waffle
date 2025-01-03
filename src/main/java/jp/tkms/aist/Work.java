package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.io.*;
import java.util.HashMap;
import java.util.UUID;

public class Work implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String wallTimePattern = "^[0-9]+:[0-6][0-9]:[0-6][0-9]$";

    private String name;
    private File workBase;
    private String wallTime;

    private HashMap<UUID, ExpSet> expSetMap;
    private HashMap<String, String> varMap;

    @Override
    public String toString() {
        return name + "\n" +
                "# Set(" + expSetMap.size() + ")";
    }

    public Work(String name) {
        this.name = name;
        workBase = new File(Config.LOCAL_WORKBASE_DIR + "/" + name);
        wallTime = Config.DEFAULT_WALLTIME;

        expSetMap = new HashMap<>();
        varMap = new HashMap<>();
        varMap.put("$_WORKNAME", name);
    }

    public String getName() {
        return name;
    }

    public ExpSet newExpSet(CommonComponent commonComponent, String seriesName, String preScript, String postScript, String exec) {
        ExpSet expSet = new ExpSet(commonComponent, this, getName() + "_" + seriesName, preScript, postScript, exec);
        expSetMap.put(expSet.getUuid(), expSet);
        return expSet;
    }

    public String getWallTime() {
        return wallTime;
    }

    public String setWallTime(String wallTime) {
        if (wallTime.matches(wallTimePattern)) {
            this.wallTime = wallTime;
        }
        return this.wallTime;
    }

    public HashMap<UUID, ExpSet> getExpSetMap() {
        return expSetMap;
    }

    public HashMap<String, String> getVarMap() {
        return varMap;
    }

    public ExpSet getExpSet(String id) {
        return expSetMap.get(UUID.fromString(id));
    }

    public void setup() {
        workBase.mkdirs();
        File localBaseRemote = new File(Config.LOCAL_WORKBASE_DIR + "/" + name + "/REMOTE");
        localBaseRemote.mkdirs();
        String remoteBase = Config.REMOTE_WORKBASE_DIR + "/" + name;
        boolean done = false;
        while (!done) {
            try {
                SshSession sshSession = new AbciSshSession();
                sshSession.mkdir(getRemoteWorkBase(), Config.REMOTE_WORKBASE_DIR);
                sshSession.scp(localBaseRemote, remoteBase, Config.REMOTE_WORKBASE_DIR);
                sshSession.disconnect();
                done = true;
            } catch (JSchException e) {
                System.out.println("SSH CONNECTION FAILED: work 1: sleep "
                        + Config.POLLING_TIME + " seconds and retry");
                try { Thread.sleep(Config.POLLING_TIME); } catch (InterruptedException e2) { }
                continue;
            }
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

    public ExecuteResult execOnLocal(String... commands) {
        int res = -1;
        String out = "";
        File exeFile = new File(workBase.getPath() + "/" + commands[0]);
        if (exeFile.exists()) {
            if (!exeFile.canExecute()) {
                exeFile.setExecutable(true);
            }
            commands[0] = exeFile.getAbsolutePath();
        }
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(commands, null, workBase);
            process.waitFor();
            res = process.exitValue();

            BufferedReader brOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            try {
                for (; ; ) {
                    String line = brOut.readLine();
                    if (line == null) break;
                    out += line + "\n";
                }
            } catch (IOException e) {
            } finally {
                brOut.close();
            }

            BufferedReader brErr = new BufferedReader(new InputStreamReader(process.getInputStream()));
            try {
                for (;;) {
                    String line = brErr.readLine();
                    if (line == null) break;
                    out += line + "\n";
                }
            } catch (IOException e) {
            } finally {
                brErr.close();
            }

            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new ExecuteResult(res, out, null);
    }

    public ExecuteResult execOnRemote(String... commands) {
        int res = -1;
        String out = "";

        String arg = "if [ -e " +commands[0]+ " -a ! -x " +commands[0]+ " ]; then chmod a+x " + commands[0] + ";fi;";
        for (String a: commands) { arg += " " + a; }

        boolean done = false;
        while (!done) {
            try {
                SshSession ssh = new AbciSshSession();
                SshChannel channel = ssh.exec(arg, getRemoteWorkBase());
                res = channel.getExitStatus();
                out += channel.getStdout();
                out += channel.getStderr();
                ssh.disconnect();
                done = true;
            } catch (JSchException e) {
                System.out.println("SSH CONNECTION FAILED: work 2: sleep "
                        + Config.POLLING_TIME + " seconds and retry");
                try { Thread.sleep(Config.POLLING_TIME); } catch (InterruptedException e2) { }
                continue;
            }
        }

        return new ExecuteResult(res, out, null);
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
