package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.util.UUID;

public class Exp {
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
        for (int c = 0; c <= Config.MAX_CAT_RECHECK; c++) {
            ssh.exec("cat " + getUuid().toString() + "/_output.json");
            if (ssh.getExitStatus() == 0) {
                status = Status.FINISHED;
                setResult(ssh.getStdout());
                resultSubmit();

                System.out.println("Exp[" + uuid.toString() + "/" + getUuid().toString() + "]");
                System.out.println(ssh.getStdout());
                break;
            } else {
                status = Status.FAILED;

                System.out.println("Exp[" + uuid.toString() + "/" + getUuid().toString() + "]");
            }
        }
    }

    public void resultSubmit() {
        if (status == Status.FINISHED) {
            ResultSubmitter.post(getExpSet().seriesName, getResult());
        }
    }
}
