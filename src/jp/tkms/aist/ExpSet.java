package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExpSet extends Thread implements Serializable {
    private static final long serialVersionUID = 1L;

    CommonComponent commonComponent;
    Work work;
    private UUID uuid;
    String seriesName;
    String preScript;
    String postScript;
    String exec;
    ArrayList<Exp> expList;
    ArrayList<ExpPack> expPackList;
    private int rerunCount;

    @Override
    public String toString() {
        return "@" + uuid.toString() + "\n" +
                "# " + seriesName + ": Pack(" + expPackList.size() + "), Exp(" + expList.size() + ")";
    }

    public ExpSet(CommonComponent commonComponent, Work work, String seriesName, String preScript, String postScript, String exec, ArrayList<Exp> expList) {
        uuid = UUID.randomUUID();
        this.commonComponent = commonComponent;
        this.work = work;
        this.seriesName = seriesName;
        this.preScript = preScript;
        this.postScript = postScript;
        this.exec = exec;
        this.expList = expList;
        rerunCount = 0;
    }

    public ExpSet(CommonComponent commonComponent, Work work, String seriesName, String preScript, String postScript, String exec) {
        this(commonComponent, work, seriesName, preScript, postScript, exec, new ArrayList<>());
    }

    public void addExp(Exp exp) {
        exp.setExpSet(this);
        expList.add(exp);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Work getWork() {
        return work;
    }

    private ArrayList<ExpPack> makeExpPacks() {
        ArrayList<ExpPack> expPackList = new ArrayList<>();

        ArrayList<Exp> nonFinishedExpList = new ArrayList<>();
        for (Exp exp : expList) {
            if (exp.getStatus() != Exp.Status.FINISHED && exp.getStatus() != Exp.Status.SUBMITTED) {
                nonFinishedExpList.add(exp);
            }
        }

        for (int i = 0; i < nonFinishedExpList.size();) {
            int packSize = AbciResourceSelector.getPackSize(nonFinishedExpList.size() - i);
            ExpPack expPack = new ExpPack(this);
            for (int n = 0; n < packSize && i < nonFinishedExpList.size(); n++) {
                expPack.addExp(nonFinishedExpList.get(i++));
            }
            expPackList.add(expPack);
        }

        return expPackList;
    }

    @Override
    public void run() {
        if (rerunCount <= 0) {
            Daemon.getInstance(commonComponent).eval(work, preScript);
        }

        expPackList = makeExpPacks();

        try {
            SshSession ssh = new AbciSshSession();
            ExecutorService exec = Executors.newFixedThreadPool(commonComponent.getMaxSshChannel());
            for (ExpPack expPack : expPackList) {
                exec.submit(new Submitter(expPack, commonComponent.getPollingMonitor(), ssh));
            }
            exec.shutdown();
            exec.awaitTermination(1, TimeUnit.DAYS);
            ssh.disconnect();
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void updateResults() {
        int finishedPack = 0;
        for (ExpPack expPack : expPackList) {
            if (expPack.getStatus() == ExpPack.Status.FINISHED) {
                finishedPack++;
            }
        }
        if (finishedPack >= expPackList.size()) {
            int finishedExp = 0;
            for (Exp exp : expList) {
                if (exp.getStatus() == Exp.Status.FINISHED) {
                    finishedExp++;
                }
            }
            if (finishedExp >= expList.size()) {
                Daemon.getInstance(commonComponent).eval(work, postScript);
            } else {
                rerunCount++;
                if (rerunCount <= Config.MAX_RERUN) {
                    this.start();
                }
            }
        }
    }

    class Submitter implements Runnable {
        private ExpPack expPack;
        private PollingMonitor pollingMonitor;
        private SshSession sshSession;

        public Submitter (ExpPack expPack, PollingMonitor pollingMonitor, SshSession sshSession) {
            this.expPack = expPack;
            this.pollingMonitor = pollingMonitor;
            this.sshSession = sshSession;
        }

        public void run() {
            try {
                expPack.run(pollingMonitor, sshSession);
            } catch (JSchException e) {
                e.printStackTrace();
            }
        }
    }
}
