package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.io.Serializable;
import java.util.ArrayList;

public class PollingMonitor extends Thread implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean isAlive = false;
    private ArrayList<ExpPack> expPackList = new ArrayList<>();
    private String prevQstatText = "FIRST_TIME";

    public void addExpPack(ExpPack expPack) {
        expPackList.add(expPack);
    }

    public ArrayList<ExpPack> getExpPackList() {
        return expPackList;
    }

    public void forceCheck() { prevQstatText = "FORCE_CHECK"; }

    public void shutdown() {
        isAlive = false;
    }

    public boolean isStoped() {
        return getState() == State.TERMINATED;
    }

    @Override
    public synchronized void start() {
        isAlive = true;
        super.start();
    }

    @Override
    public void run() {
        while (isAlive) {
            if (!expPackList.isEmpty()) {
                SshSession ssh = null;
                try {
                    ssh = new AbciSshSession();
                } catch (JSchException e) {
                    //e.printStackTrace();
                    System.out.println("SSH CONNECTION FAILED: polling 1: sleep "
                            + Config.POLLING_TIME + " seconds and retry");
                    try { Thread.sleep(Config.POLLING_TIME); } catch (InterruptedException e2) { }
                    continue;
                }

                if (ssh != null) {
                    ArrayList<ExpPack> currentExpPackList = new ArrayList<>(expPackList);

                    SshChannel ch = null;
                    while (ch == null) {
                        try {
                            ch = ssh.exec("qstat", "~/");
                        } catch (JSchException e) {
                            //e.printStackTrace();
                            ch = null;
                            System.out.println("SSH CONNECTION FAILED: polling 2: sleep "
                                    + Config.POLLING_TIME + " seconds and retry");
                            try { Thread.sleep(Config.POLLING_TIME); } catch (InterruptedException e2) { }
                            continue;
                        }
                    }
                    if (ch.getStdout().equals("") || !prevQstatText.equals(ch.getStdout())) {
                        prevQstatText = ch.getStdout();

                        for (ExpPack expPack : currentExpPackList) {
                            if (!isAlive) { break; }

                            ch = null;
                            while (ch == null) {
                                try {
                                    ch = ssh.exec("qstat -j " + expPack.getJobId(), "~/");
                                } catch (JSchException e) {
                                    //e.printStackTrace();
                                    ch = null;
                                    System.out.println("SSH CONNECTION FAILED: polling 3: sleep "
                                            + Config.POLLING_TIME + " seconds and retry");
                                    try { Thread.sleep(Config.POLLING_TIME); } catch (InterruptedException e2) { }
                                    continue;
                                }
                            }
                            //System.out.println("Polling Result (qstat): " + ch.getExitStatus());

                            if (ch.getExitStatus() == 1) {
                                ch = null;
                                while (ch == null) {
                                    try {
                                        ch = ssh.exec("qacct -j " + expPack.getJobId(), "~/");
                                    } catch (JSchException e) {
                                        //e.printStackTrace();
                                        ch = null;
                                        System.out.println("SSH CONNECTION FAILED: polling 4: sleep "
                                                + Config.POLLING_TIME + " seconds and retry");
                                        try { Thread.sleep(Config.POLLING_TIME); } catch (InterruptedException e2) { }
                                        continue;
                                    }
                                }
                                //System.out.println("Polling Result (qacct): " + ch.getExitStatus());

                                if (ch.getExitStatus() == 0) {
                                    expPack.updateResults(ssh);
                                    expPackList.remove(expPack);
                                } else {
                                    prevQstatText += "@QACCT";
                                }
                            }
                        }

                        System.out.print("+");
                    } else {
                        System.out.print("-");
                    }
                }

                ssh.disconnect();
            } else {
                //System.out.print(".");
            }

            try { Thread.sleep(Config.POLLING_TIME); } catch (InterruptedException e) { }
        }
        System.out.println("PollingMonitor terminated");
    }
}
