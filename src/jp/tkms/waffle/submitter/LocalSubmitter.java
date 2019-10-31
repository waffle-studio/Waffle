package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Run;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LocalSubmitter extends AbstractSubmitter {
  @Override
  String getWorkDirectory(Run run) {
    Host host = run.getHost();
    String pathString = (host.getWorkBaseDirectory() + host.getDirectorySeparetor() + run.getId());

    try {
      Files.createDirectories(Paths.get(pathString));
    } catch (IOException e) {
      e.printStackTrace();
    }



    return pathString;
  }

  @Override
  void submitProcess(Run run) {
  }
}
