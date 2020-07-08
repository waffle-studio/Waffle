package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.exception.FailedToTransferFileException;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.submitter.AbstractSubmitter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Remote {
  SimulatorRun run;
  AbstractSubmitter submitter;

  public Remote(SimulatorRun run, AbstractSubmitter submitter) {
    this.run = run;
    this.submitter = submitter;
  }

  public String getFileContents(String path) {
    String content = "";
    try {
      content =  submitter.getFileContents(run, path);
    } catch (Exception | Error e) {
      WarnLogMessage.issue(run, e);
    }
    return content;
  }

  public void pull(String path) {
    try {
      Path local = run.getWorkPath().resolve(path);
      submitter.transferFile(Paths.get(submitter.getWorkDirectory(run)).resolve(path).toString(), local);
    } catch (FailedToTransferFileException e) {
      WarnLogMessage.issue(run, e);
    }
  }
}
