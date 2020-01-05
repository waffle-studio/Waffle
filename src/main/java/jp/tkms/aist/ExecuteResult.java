package jp.tkms.aist;

public class ExecuteResult {
    private int exitCode;
    private String out;
    private String error;

    public ExecuteResult(int exitCode, String out, String error) {
        this.exitCode = exitCode;
        this.out = out;
        this.error = error;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOut() {
        return out;
    }

    public String getError() {
        return error;
    }
}
