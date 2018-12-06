package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.util.ArrayList;

public class PollingMonitor extends Thread {
    private boolean isAlive = false;
    private ArrayList<ExpPack> expPackList = new ArrayList<>();

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

                    for (ExpPack expPack : currentExpPackList) {
                        try {
                            ssh.exec("qstat -j " + expPack.getJobId());
                            //System.out.println("Polling Result (qstat): " + ssh.getExitStatus());

                            if (ssh.getExitStatus() == 1) {
                                ssh.exec("qacct -j " + expPack.getJobId());
                                //System.out.println("Polling Result (qacct): " + ssh.getExitStatus());

                                if (ssh.getExitStatus() == 0) {
                                    expPack.updateResults(ssh);
                                    finishedExpPackList.add(expPack);
                                }
                            }
                        } catch (JSchException e) {
                            e.printStackTrace();
                        }
                    }

                    expPackList.removeAll(finishedExpPackList);
                }

                ssh.disconnect();
            }

            //System.out.println("Polling...");
            System.out.print(".");
            try { Thread.sleep(Config.POLLING_TIME); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}
