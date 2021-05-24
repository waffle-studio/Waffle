package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ResourceFile {
  private static ResourceFile staticInstance = new ResourceFile();

  private static final InstanceCache<String, String> instanceCache = new InstanceCache<>();

  public static String getContents(String path) {
    String candidate = instanceCache.get(path);
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

    instanceCache.put(path, contents.toString());
    return contents.toString();
  }

  public static void unzip(String fileName, Path destPath) {
    try (ZipInputStream zipInputStream = new ZipInputStream(staticInstance.getClass().getResourceAsStream(fileName))) {
      ZipEntry entry = null;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        Path entryPath = destPath.resolve(entry.getName());
        if (entry.isDirectory()) {
          Files.createDirectories(entryPath);
        } else {
          Files.createDirectories(entryPath.getParent());
          try (OutputStream out = new FileOutputStream(entryPath.toFile())){
            IOUtils.copy(zipInputStream, out);
          }
        }
      }
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }
  }
}
