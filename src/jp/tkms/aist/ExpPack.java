package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.util.ArrayList;
import java.util.UUID;

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

    public void run(PollingMonitor monitor) throws JSchException {
        if (expList.isEmpty()) { return; }

        String workDir = Config.WORKBASE_DIR + uuid.toString() + "/";

        SshSession ssh = new AbciSshSession();

        ssh.exec("mkdir -p " + workDir);
        ssh.setWorkDir(workDir);

        ssh.exec("rm -f run.txt");
        for (Exp exp : expList) {
            ssh.exec("echo '" + exp.getUuid().toString() + " " +
                    "\"" + exp.getExpSet().script + "\" " + exp.getArgs() + "' >> run.txt");
            exp.setStatus(Exp.Status.SUBMITTED);
        }

        ssh.exec("echo '#!/bin/bash\n\n" +
                "mkdir -p $1\n" +
                "cd $1\n" +
                "CMD=$2\n" +
                "shift 2\n" +
                "$CMD $*\n' > run.sh");
        ssh.exec("chmod a+x run.sh");

        ssh.exec("echo '#!/bin/bash\n\n" +
                "#$ -l " + AbciResourceSelector.getResourceText(expList.size()) + "\n" +
                "#$ -l h_rt=" + Config.WALLTIME + "\n" +
                "#$ -o ' \"`pwd`/abci-stdout.txt\" '\n" +
                "#$ -j y\n\n' > batch.sh");

        ssh.exec("echo 'source ~/.bash_profile' >> batch.sh");
        ssh.exec("echo 'cd " + workDir + "' >> batch.sh");

        ssh.exec("echo 'xargs -a run.txt -P " + expList.size() + " -L 1 ./run.sh' >> batch.sh");

        ssh.exec("chmod a+x batch.sh");
        ssh.exec("qsub -g " + Config.GROUP_ID + " batch.sh");
        String jobId = ssh.getStdout().replaceAll("[\r\n]", " ").replaceFirst("Your job (\\d*) .*", "$1");
        System.out.println("[" + jobId + "] " + expList.size());
        this.jobId = jobId;

        ssh.disconnect();

        status = Status.SUBMITTED;

        monitor.addExpPack(this);
    }

    public void updateResults(SshSession ssh) throws JSchException {
        String workDir = Config.WORKBASE_DIR + uuid.toString() + "/";
        ssh.setWorkDir(workDir);

        for (Exp exp : expList) {
            exp.updateResult(ssh);
        }

        status = Status.FINISHED;
    }
}
