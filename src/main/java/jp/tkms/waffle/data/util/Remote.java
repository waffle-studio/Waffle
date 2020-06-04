package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.submitter.AbstractSubmitter;

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
}
