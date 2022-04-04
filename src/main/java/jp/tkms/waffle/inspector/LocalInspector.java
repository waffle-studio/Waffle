package jp.tkms.waffle.inspector;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.sub.servant.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class LocalInspector extends Inspector {
  LocalInspector(Mode mode, Computer computer) {
    super(mode, computer);

    Path notifierPath = Paths.get(computer.getWorkBaseDirectory()).resolve(Constants.NOTIFIER);
    try {
      Files.writeString(notifierPath, UUID.randomUUID().toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
    Main.registerFileChangeEventListener(notifierPath.getParent(), () -> {
      notifyUpdate();
    });
  }

  public void notifyUpdate(){
    waitCount = toMilliSecond(computer.getPollingInterval()) - waitingStep;
  }
}
