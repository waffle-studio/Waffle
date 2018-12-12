package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.io.Serializable;
import java.util.ArrayList;

public class PollingMonitor extends Thread implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean isAlive = false;
    private ArrayList<ExpPack> expPackList = new ArrayList<>();
    private String prevQstatText = "";

    public void addExpPack(ExpPack expPack) {
        expPackList.add(expPack);
    }

    public void shutdown() {
        isAlive = false;
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
                    ArrayList<ExpPack> finishedExpPackList = new ArrayList<>();

                    try {
                        SshChannel ch = ssh.exec("qstat", "~/");
                        if (!prevQstatText.equals(ch.getStdout())) {
                            prevQstatText = ch.getStdout();

                            for (ExpPack expPack : currentExpPackList) {
                                ch = ssh.exec("qstat -j " + expPack.getJobId(), "~/");
                                //System.out.println("Polling Result (qstat): " + ch.getExitStatus());

                                if (ch.getExitStatus() == 1) {
                                    ch = ssh.exec("qacct -j " + expPack.getJobId(), "~/");
                                    //System.out.println("Polling Result (qacct): " + ch.getExitStatus());

                                    if (ch.getExitStatus() == 0) {
                                        expPack.updateResults(ssh);
                                        finishedExpPackList.add(expPack);
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

                    expPackList.removeAll(finishedExpPackList);
                }

                ssh.disconnect();
            } else {
                System.out.print(".");
            }

            try { Thread.sleep(Config.POLLING_TIME); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}
