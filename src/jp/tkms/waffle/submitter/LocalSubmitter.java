package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.ParameterExtractor;
import jp.tkms.waffle.data.Run;
import jp.tkms.waffle.extractor.AbstractParameterExtractor;
import jp.tkms.waffle.extractor.RubyParameterExtractor;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LocalSubmitter extends AbstractSubmitter {
  @Override
  String getWorkDirectory(Run run) {
    Host host = run.getHost();
    String pathString = host.getWorkBaseDirectory() + host.getDirectorySeparetor()
      + RUN_DIR + host.getDirectorySeparetor() + run.getId();

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
      PrintWriter pw = new PrintWriter(new BufferedWriter(
        new FileWriter(getWorkDirectory(run) + run.getHost().getDirectorySeparetor() + BATCH_FILE)
      ));
      pw.println(makeBatchFileText(run));
      pw.close();

      for (ParameterExtractor extractor : ParameterExtractor.getList(run.getSimulator())) {
        AbstractParameterExtractor instance = AbstractParameterExtractor.getInstance(RubyParameterExtractor.class.getCanonicalName());
        instance.extract(run, extractor, this);
      }

      pw = new PrintWriter(new BufferedWriter(
        new FileWriter(getWorkDirectory(run) + run.getHost().getDirectorySeparetor() + ARGUMENTS_FILE)
      ));
      pw.println(makeArgumentFileText(run));
      pw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String exec(Run run, String command) {
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
  int getExitStatus(Run run) {
    int status = -1;

    try {
      FileReader file
        = new FileReader(getWorkDirectory(run) + run.getHost().getDirectorySeparetor() + EXIT_STATUS_FILE);
      BufferedReader r  = new BufferedReader(file);
      String line;
      while ((line = r.readLine()) != null) {
        status = Integer.valueOf(line);
        break;
      }

      r.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return status;
  }

  @Override
  void postProcess(Run run) {
    try {
      deleteDirectory(getWorkDirectory(run));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() {

  }

  @Override
  public String getFileContents(Run run, String path) {
    return exec(run, "cat " + getContentsPath(run, path));
  }

  public static void deleteDirectory(final String dirPath) throws Exception {
    File file = new File(dirPath);
    recursiveDeleteFile(file);
  }

  private static void recursiveDeleteFile(final File file) throws Exception {
    if (!file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        recursiveDeleteFile(child);
      }
    }
    file.delete();
  }
}
