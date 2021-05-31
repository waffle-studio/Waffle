package jp.tkms.waffle.sub.servant;

import jp.tkms.waffle.sub.servant.processor.RequestProcessor;

import java.nio.file.Files;
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
        Path envelopePath = Paths.get(args[2]);
        Envelope request = Envelope.loadAndExtract(baseDirectory, envelopePath);
        //request.getMessageBundle().print("SERVANT");
        Files.delete(envelopePath);
        Envelope response = new Envelope(baseDirectory);
        RequestProcessor.processMessages(baseDirectory, request, response);
        response.save(Envelope.getResponsePath(envelopePath));
        break;
      case "exec":
        if (args.length < 3) {
          exitWithInvalidArgumentsMessage("exec", "[TASK JSON PATH]");
        }
        TaskExecutor executor = new TaskExecutor(baseDirectory, Paths.get(args[2]));
        executor.execute();
        break;
      case "terminal":
        if (args.length < 3) {
          exitWithInvalidArgumentsMessage("terminal", "[TERMINAL ID]");
        }
        PseudoTerminal pseudoTerminal = new PseudoTerminal(baseDirectory, args[2]);
        pseudoTerminal.run();
        break;
      default:
        exitWithInvalidArgumentsMessage("", "");
    }

    return;
  }

  private static void exitWithInvalidArgumentsMessage(String mode, String additionalOption) {
    System.err.println("usage: java -jar <JAVA JAR> [BASE DIRECTORY] "
      + (mode == null || mode.equals("") ? "[MODE{main,exec,terminal}]" : mode) + " " + additionalOption);
    System.exit(1);
  }
}
