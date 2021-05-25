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
      case "exec":
        if (args.length < 3) {
          exitWithInvalidArgumentsMessage("exec", "[TASK JSON PATH]");
        }
        TaskExecutor executor = new TaskExecutor(baseDirectory, Paths.get(args[2]));
        executor.execute();
        break;
      default:
        exitWithInvalidArgumentsMessage("", "");
    }

    return;
  }

  private static void exitWithInvalidArgumentsMessage(String mode, String additionalOption) {
    System.err.println("usage: java -jar <JAVA JAR> [BASE DIRECTORY] "
      + (mode == null || mode.equals("") ? "[MODE{main,exec}]" : mode) + " " + additionalOption);
    System.exit(1);
  }
}
