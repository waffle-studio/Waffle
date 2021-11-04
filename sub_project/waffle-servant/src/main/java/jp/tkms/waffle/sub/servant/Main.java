package jp.tkms.waffle.sub.servant;

import jp.tkms.waffle.sub.servant.processor.RequestProcessor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
        setupBinDirectory(baseDirectory);
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
      case "put_result":
        if (args.length < 4) {
          exitWithInvalidArgumentsMessage("put_result", "[KEY] [VALUE]");
        }
        EventRecorder.printEventString(args[2], args[3]);
        break;
      case "push_file":
        if (args.length < 4) {
          exitWithInvalidArgumentsMessage("push_file", "[TASK JSON PATH] [FILE NAME]");
        }
        (new PushFileCommand(baseDirectory, Paths.get(args[2]), Paths.get(args[3]))).run();
        break;
      case "get_value":
        boolean isSuccess = false;
        if (args.length == 4) {
          isSuccess = (new GetValueCommand(baseDirectory, Paths.get(args[2]), args[3])).run();
        } else if (args.length == 6) {
          isSuccess = (new GetValueCommand(baseDirectory, Paths.get(args[2]), args[3], args[4], args[5])).run();
        } else if (args.length >= 7) {
          isSuccess = (new GetValueCommand(baseDirectory, Paths.get(args[2]), args[3], args[4], args[5], Integer.parseInt(args[6]))).run();
        } else {
          exitWithInvalidArgumentsMessage("get_value", "[TASK JSON PATH] [KEY] [FILTER OP] [FILTER VAL] [TIMEOUT]");
        }
        if (!isSuccess) {
          System.exit(1);
        }
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

  private static void setupBinDirectory(Path baseDirectory) {
    Path binDirectory = getBinDirectory(baseDirectory);
    if (!Files.exists(binDirectory)) {
      try {
        createWaffleCommandFile(binDirectory.resolve("waffle-push"), baseDirectory, "push_file ${" + Constants.WAFFLE_TASK_JSONFILE + "} \"$@\"");
        createWaffleCommandFile(binDirectory.resolve("waffle-pull"), baseDirectory, "pull_file  ${" + Constants.WAFFLE_TASK_JSONFILE + "} \"$@\"");
        createWaffleCommandFile(binDirectory.resolve("waffle-put"), baseDirectory, "put_result \"$@\"");
        createWaffleCommandFile(binDirectory.resolve("waffle-get"), baseDirectory, "get_value ${" + Constants.WAFFLE_TASK_JSONFILE + "} \"$@\"");
      } catch (IOException | URISyntaxException e) {
        e.printStackTrace();
      }
    }
  }

  private static void createWaffleCommandFile(Path path, Path baseDirectory, String options) throws IOException, URISyntaxException {
    final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if(!currentJar.getName().endsWith(".jar")) { return; }

    writeExecutableFile(path, "#!/bin/sh\n\"" + javaBin + "\"" +
      " --illegal-access=deny --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED" +
      " -jar \"" + currentJar.toPath().toAbsolutePath() + "\" \"" + baseDirectory.toAbsolutePath() + "\" " + options + '\n');
  }

  private static void writeExecutableFile(Path path, String contents) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(path, contents);
    path.toFile().setExecutable(true, false);
  }

  public static Path getBinDirectory(Path baseDirectory) {
    return baseDirectory.resolve("bin");
  }
}
