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
                    e.printStackTrace();
                }

                if (ssh != null) {
                    ArrayList<ExpPack> currentExpPackList = new ArrayList<>(expPackList);

                    try {
                        SshChannel ch = ssh.exec("qstat", "~/");
                        if (ch.getStdout().equals("") || !prevQstatText.equals(ch.getStdout())) {
                            prevQstatText = ch.getStdout();

                            for (ExpPack expPack : currentExpPackList) {
                                if (!isAlive) { break; }

                                ch = ssh.exec("qstat -j " + expPack.getJobId(), "~/");
                                //System.out.println("Polling Result (qstat): " + ch.getExitStatus());

                                if (ch.getExitStatus() == 1) {
                                    ch = ssh.exec("qacct -j " + expPack.getJobId(), "~/");
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
                    } catch (JSchException e) {
                        e.printStackTrace();
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
