package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import spark.utils.IOUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ResourceFile {
  private static ResourceFile staticInstance = new ResourceFile();

  private static final HashMap<String, String> instanceMap = new HashMap<>();

  public static String getContents(String path) {
    String candidate = instanceMap.get(path);
    if (candidate != null) {
      return candidate;
    }

    StringBuilder contents = new StringBuilder();
    InputStream in = staticInstance.getClass().getResourceAsStream(path);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String data;
    try {
      while (true) {
        if (!((data = reader.readLine()) != null)) break;
        contents.append(data).append('\n');
      }
      reader.close();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
      return "";
    }

    instanceMap.put(path, contents.toString());
    return contents.toString();
  }

  public static void unzip(String fileName, Path destPath) {
    try (ZipFile zipFile = new ZipFile(new File(staticInstance.getClass().getResource(fileName).toURI()))) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        Path entryPath = destPath.resolve(entry.getName());
        if (entry.isDirectory()) {
          Files.createDirectories(entryPath);
        } else {
          Files.createDirectories(entryPath.getParent());
          try (InputStream in = zipFile.getInputStream(entry)){
            try (OutputStream out = new FileOutputStream(entryPath.toFile())){
              IOUtils.copy(in, out);
            }
          }
        }
      }
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }
  }
}
