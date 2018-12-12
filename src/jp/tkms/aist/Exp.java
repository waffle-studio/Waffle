package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.io.Serializable;
import java.util.UUID;

public class Exp implements Serializable {
    private static final long serialVersionUID = 1L;

    public static enum Status{
        CREATED,
        SUBMITTED,
        FINISHED,
        FAILED
    }

    private UUID uuid;
    private ExpSet expSet;
    private String args;
    private ExpPack expPack;

    private Status status;
    private String result;

    public Exp(String args) {
        uuid = UUID.randomUUID();
        this.args = args;
        status = Status.CREATED;
        result = "";
    }

    public Exp(ExpSet expSet, String args) {
        this(args);
        this.expSet = expSet;
    }

    public void setExpSet(ExpSet expSet) {
        this.expSet = expSet;
    }

    public void setExpPack(ExpPack expPack) {
        this.expPack = expPack;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public ExpSet getExpSet() {
        return expSet;
    }

    public String getArgs() {
        return args;
    }

    public ExpPack getExpPack() {
        return expPack;
    }

    public Status getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void updateResult(SshSession ssh) throws JSchException {
        String workDir = Config.REMOTE_WORKDIR + "/" + getExpPack().getUuid().toString() + "/";

        for (int c = 0; c <= Config.MAX_CAT_RECHECK; c++) {
            SshChannel ch = ssh.exec("cat " + getUuid().toString() + "/_output.json", workDir);
            if (ch.getExitStatus() == 0) {
                status = Status.FINISHED;
                setResult(ch.getStdout());
                resultSubmit();

                System.out.println("Exp[" + getExpPack().getUuid().toString() + "/" + getUuid().toString() + "]");
                System.out.println(ch.getStdout());
                break;
            } else {
                status = Status.FAILED;

                System.out.println("Exp[" + getExpPack().getUuid().toString() + "/" + getUuid().toString() + "]");
            }
        }
    }

    public void resultSubmit() {
        if (status == Status.FINISHED) {
            Submitter submitter = new Submitter();
            submitter.start();
        }
    }

    private class Submitter extends Thread {
        @Override
        public void run() {
            ResultSubmitter.post(getExpSet().seriesName, getResult());
        }
    }

    @Override
    public String toString() {
        return "(Exp:" + uuid.toString() + ") " + args;
    }
}
