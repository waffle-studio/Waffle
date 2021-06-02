package jp.tkms.waffle.sub.servant;

import org.apache.commons.io.IOUtils;

import javax.xml.stream.events.Comment;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class XsubFile {
  private static final String XSUB_ZIP = "/xsub.zip";
  private static final Path XSUB_BIN = Paths.get("xsub").resolve("bin");
  private static final String XSUB = "xsub";
  private static final String XSTAT = "xstat";
  private static final String XDEL = "xdel";

  private static XsubFile staticInstance = new XsubFile();

  private static Path getPath(Path baseDirectory, String command) throws IOException {
    Path path = baseDirectory.resolve(XSUB_BIN).resolve(command);
    if (!Files.exists(path)) {
      synchronized (staticInstance) {
        if (!Files.exists(path)) {
          unzip(baseDirectory);
        }
      }
    }
    return path;
  }

  public static Path getXsubPath(Path baseDirectory) throws IOException {
    return getPath(baseDirectory, XSUB);
  }

  public static Path getXstatPath(Path baseDirectory) throws IOException {
    return getPath(baseDirectory, XSTAT);
  }

  public static Path getXdelPath(Path baseDirectory) throws IOException {
    return getPath(baseDirectory, XDEL);
  }

  private static void unzip(Path baseDirectory) throws IOException {
    try (ZipInputStream zipInputStream = new ZipInputStream(staticInstance.getClass().getResourceAsStream(XSUB_ZIP))) {
      ZipEntry entry = null;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        Path entryPath = baseDirectory.resolve(entry.getName());
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
      throw e;
    }
  }
}
