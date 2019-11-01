package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Run;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LocalSubmitter extends AbstractSubmitter {
  @Override
  String getWorkDirectory(Run run) {
    Host host = run.getHost();
    String pathString = host.getWorkBaseDirectory() + host.getDirectorySeparetor()
      + run.getId() + host.getDirectorySeparetor();

    try {
      Files.createDirectories(Paths.get(pathString + host.getDirectorySeparetor()));
    } catch (IOException e) {
      e.printStackTrace();
    }

    return pathString;
  }

  @Override
  void prepare(Run run) {
    try {
      FileWriter file
        = new FileWriter(getWorkDirectory(run) + run.getHost().getDirectorySeparetor() + BATCH_FILE);
      PrintWriter pw = new PrintWriter(new BufferedWriter(file));

      pw.println(makeBatchFileText(run));

      pw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  String exec(Run run, String command) {
    String result = "";
    ProcessBuilder p = new ProcessBuilder("sh", "-c", command);
    p.redirectErrorStream(true);

    try {
      Process process = p.start();

      try (BufferedReader r
             = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
        String line;
        while ((line = r.readLine()) != null) {
          result += line + "\n";
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  @Override
  public void close() {

  }
}
