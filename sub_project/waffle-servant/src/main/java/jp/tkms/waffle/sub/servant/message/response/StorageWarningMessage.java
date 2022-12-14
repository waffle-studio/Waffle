package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.JobMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class StorageWarningMessage extends AbstractResponseMessage {
  String message;

  public StorageWarningMessage() { }

  public StorageWarningMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public static void addMessageIfCritical(Envelope envelope) {
    try {
      String line = "";
      Process process = Runtime.getRuntime().exec("df -h --total --output=avail,pcent,iavail,ipcent . | tail -n 1");
      process.waitFor();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        line = reader.readLine();
      }
      if (line.matches(".*(100|99).*%")) {
        envelope.add(new StorageWarningMessage("Storage is full. " + line));
      }
    } catch (Exception e) {
    }
  }
}
