package jp.tkms.waffle.sub.servant;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      exitWithInvalidArgumentsMessage("", "");
    }

    Path baseDirectory = Paths.get(args[0]);

    switch (args[1].toLowerCase(Locale.ROOT)) {
      case "main":
        if (args.length < 3) {
          exitWithInvalidArgumentsMessage("main", "[MESSAGE PATH]");
        }
        Envelope envelope = Envelope.loadAndExtract(baseDirectory, Paths.get(args[2]));
        break;
      case "event":
        if (args.length < 3) {
          exitWithInvalidArgumentsMessage("event", "[RECORD PATH]");
        }
        EventRecorder eventRecorder = new EventRecorder(baseDirectory, Paths.get(args[2]));
        eventRecorder.record();
        break;
    }

    return;
  }

  private static void exitWithInvalidArgumentsMessage(String mode, String additionalOption) {
    System.err.println("usage: java -jar <JAVA JAR> [BASE DIRECTORY] "
      + (mode == null || mode.equals("") ? "[MODE{main}]" : mode) + " " + additionalOption);
    System.exit(1);
  }
}
