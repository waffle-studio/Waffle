package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Run;

abstract public class AbstractSubmitter {
  abstract String getWorkDirectory(Run run);
  abstract void submitProcess(Run run);

  static void submit(Run run) {
    AbstractSubmitter submitter = null;
    Host host = run.getHost();

    if (host.isLocal()) {
      submitter = new LocalSubmitter();
    }

    submitter.submitProcess(run);
  }

  String makeBatchFileText() {
    return "#!/bin/sh\n" +
      "\n" +
      "";
  }
}
