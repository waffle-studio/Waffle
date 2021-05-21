package jp.tkms.waffle.sub.servant;

import jp.tkms.waffle.sub.servant.message.AbstractMessage;

import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Envelope {
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final String MESSAGE_BUNDLE = MessageBundle.class.getSimpleName();
  private static final Path FILES = Paths.get("FILES");

  private Path baseDirectory;
  private MessageBundle messageBundle;
  private ArrayList<Path> filePathList;

  public Envelope(Path baseDirectory) {
    this.baseDirectory = baseDirectory;
    messageBundle = new MessageBundle();
    filePathList = new ArrayList<>();
  }

  public void add(AbstractMessage message) {
    messageBundle.add(message);
  }

  public void add(Path path) {
    if (path.isAbsolute()) {
      filePathList.add(baseDirectory.relativize(path));
    } else {
      filePathList.add(path);
    }
  }

  public void save(Path path) throws Exception {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(path.toFile()), UTF8)) {
      zipOutputStream.putNextEntry(new ZipEntry(MESSAGE_BUNDLE));
      messageBundle.serialize(zipOutputStream);
      zipOutputStream.closeEntry();

      zipOutputStream.putNextEntry(createDirectoryZipEntry(FILES));
      zipOutputStream.closeEntry();


/*

      ZipEntry entry = new ZipEntry(MESSAGE_BUNDLE);
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

 */
    } catch (Exception e) {
      throw e;
    }
  }

  private String pathToString(Path path) {
    // return path.toString(); // this method is not compatible to Windows
    StringBuilder stringBuilder = new StringBuilder();
    for (Path p : path.normalize()) {
      if (stringBuilder.length() > 0) {
        stringBuilder.append('/');
      }
      stringBuilder.append(p.toString());
    }
    return stringBuilder.toString();
  }

  private ZipEntry createDirectoryZipEntry(Path path) {
    return new ZipEntry(pathToString(path) + '/');
  }

}
