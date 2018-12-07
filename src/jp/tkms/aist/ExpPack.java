package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExpPack {
    public static enum Status{
        CREATED,
        SUBMITTED,
        FINISHED
    }

    private UUID uuid;
    private ArrayList<Exp> expList;
    private String jobId;
    private Status status;

    public ExpPack() {
        uuid = UUID.randomUUID();
        expList = new ArrayList<>();
        status = Status.CREATED;
    }

    public void addExp(Exp exp) {
        exp.setExpPack(this);
        expList.add(exp);
    }

    public String getJobId() {
        return jobId;
    }

    public Status getStatus() {
        return status;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void run(PollingMonitor monitor, SshSession ssh) throws JSchException {
        if (expList.isEmpty()) { return; }

        String workDir = Config.WORKBASE_DIR + uuid.toString() + "/";

        //SshSession ssh = new AbciSshSession();

        ssh.exec("mkdir -p " + workDir, "~/");

        ssh.exec("rm -f run.txt", workDir);
        String runtxt = "";
        for (Exp exp : expList) {
            runtxt += "echo '" + exp.getUuid().toString() + " " +
                    "\"" + exp.getExpSet().script + "\" " + exp.getArgs() + "' >> run.txt;";
            exp.setStatus(Exp.Status.SUBMITTED);
        }
        ssh.exec(runtxt, workDir);

        ssh.exec("echo '#!/bin/bash\n\n" +
                "mkdir -p $1\n" +
                "cd $1\n" +
                "RESDIR=`pwd`\n" +
                "CMD=$2\n" +
                "cd $SGE_LOCALDIR\n" +
                "mkdir -p $1\n" +
                "cd $1\n" +
                "shift 2\n" +
                "$CMD $*\n" +
                "cp _output.json ${RESDIR}/\n' > run.sh && " +
                "chmod a+x run.sh && " +
                "echo '#!/bin/bash\n\n" +
                "#$ -l " + AbciResourceSelector.getResourceText(expList.size()) + "\n" +
                "#$ -l h_rt=" + Config.WALLTIME + "\n" +
                "#$ -o ' \"`pwd`/abci-stdout.txt\" '\n" +
                "#$ -j y\n\n' > batch.sh && " +
                "chmod a+x batch.sh && " +
                "echo 'source ~/.bash_profile' >> batch.sh && " +
                "echo 'cd " + workDir + "' >> batch.sh && " +
                "echo 'xargs -a run.txt -P " + expList.size() + " -L 1 ./run.sh' >> batch.sh", workDir);

        SshChannel ch = ssh.exec("qsub -g " + Config.GROUP_ID + " batch.sh", workDir);
        String jobId = ch.getStdout().replaceAll("[\r\n]", " ").replaceFirst("Your job (\\d*) .*", "$1");
        System.out.println("[" + jobId + "] " + expList.size());
        this.jobId = jobId;

        //ssh.disconnect();

        status = Status.SUBMITTED;

        monitor.addExpPack(this);
    }

    public void updateResults(SshSession ssh) {
        ExecutorService exec = Executors.newFixedThreadPool(Config.MAX_SSH_CHANNEL);
        for (Exp exp : expList) {
            exec.submit(new Collector(exp, ssh));
        }
        exec.shutdown();
        try {
            exec.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        status = Status.FINISHED;
    }

    class Collector implements Runnable {
        private Exp exp;
        private SshSession sshSession;

        public Collector(Exp exp, SshSession sshSession) {
            this.exp = exp;
            this.sshSession = sshSession;
        }

        @Override
        public void run() {
            try {
                exp.updateResult(sshSession);
            } catch (JSchException e) {
                e.printStackTrace();
            }
        }
    }
}
