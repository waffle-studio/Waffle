package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.SimulatorRun;
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
    } catch (Exception e) {}
    return content;
  }

  public void pull(String path) {
    try {
      Path local = run.getWorkPath().resolve(path);
      submitter.transferFile(Paths.get(submitter.getWorkDirectory(run)).resolve(path).toString(), local);
    } catch (Exception e) {}
  }
}
