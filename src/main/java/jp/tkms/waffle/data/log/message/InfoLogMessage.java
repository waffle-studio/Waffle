package jp.tkms.waffle.data.log.message;

import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;

public class InfoLogMessage extends LogMessage {
  public InfoLogMessage(String message) {
    super(message);
  }

  public static void issue(String message) {
    new InfoLogMessage(message).printMessage();
  }

  public static void issue(ComputerTask run, String message) {
    new InfoLogMessage("Run(" + run.getLocalDirectoryPath().toString() + ") " + message).printMessage();
  }

  public static void issue(Computer computer, String message) {
    new InfoLogMessage("Computer(" + computer.getName() + ") " + message).printMessage();
  }
}
