package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.submitter.AbstractSubmitter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Remote {
  ExecutableRun run;
  AbstractSubmitter submitter;

  public Remote(ExecutableRun run, AbstractSubmitter submitter) {
    this.run = run;
    this.submitter = submitter;
  }

  public String getFileContents(String path) {
    String content = "";
    try {
      content =  submitter.getFileContents(run, Paths.get(path));
    } catch (Exception | Error e) {
      WarnLogMessage.issue(run, e);
    }
    return content;
  }

  public String getCommandResults(String command) {
    String content = "";
    try {
      content =  submitter.exec("cd '" + submitter.getBaseDirectory(run) + "' && " + command);
    } catch (Exception | Error e) {
      WarnLogMessage.issue(run, e);
    }
    return content;
  }

  public void pull(String path) {
    try {
      Path local = run.getBasePath().resolve(path);
      submitter.transferFilesFromRemote(submitter.getBaseDirectory(run).resolve(path), local);
    } catch (FailedToTransferFileException | FailedToControlRemoteException e) {
      WarnLogMessage.issue(run, e);
    }
  }
}
