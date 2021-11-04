package jp.tkms.waffle.sub.servant;

import jp.tkms.waffle.sub.servant.message.AbstractMessage;
import jp.tkms.waffle.sub.servant.message.response.ExceptionMessage;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Envelope {
  private static final String MESSAGE_BUNDLE = MessageBundle.class.getSimpleName();
  private static final Path FILES = Paths.get("FILES");

  private Path baseDirectory;
  private MessageBundle messageBundle;
  private ArrayList<Path> filePathList;

  public Envelope(Path baseDirectory) {
    this.baseDirectory = baseDirectory.toAbsolutePath();
    messageBundle = new MessageBundle();
    filePathList = new ArrayList<>();
  }

  public boolean isEmpty() {
    return messageBundle.isEmpty() && filePathList.isEmpty();
  }

  public void add(AbstractMessage message) {
    synchronized (messageBundle) {
      messageBundle.add(message);
    }
  }

  public void add(Path path) {
    if (path.isAbsolute()) {
      path = baseDirectory.relativize(path).normalize();
    }
    synchronized (filePathList) {
      filePathList.add(path);
    }
  }

  public MessageBundle getMessageBundle() {
    return messageBundle;
  }

  public void save(Path dataPath) throws Exception {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(dataPath.toFile()), StandardCharsets.UTF_8)) {
      Set<Path> sourceSet = new LinkedHashSet<>();
      for (Path filePath : filePathList) {
        if (Files.exists(baseDirectory.resolve(filePath))) {
          addPathsToSet(sourceSet, filePath);
        }
      }

      zipOutputStream.putNextEntry(new ZipEntry(MESSAGE_BUNDLE));
      messageBundle.serialize(zipOutputStream);
      zipOutputStream.closeEntry();

      for (Path source : sourceSet) {
        Path destination = FILES.resolve(source);
        if (Files.isDirectory(baseDirectory.resolve(source))) {
          zipOutputStream.putNextEntry(createDirectoryZipEntry(destination));
        } else {
          try (InputStream in = new FileInputStream(baseDirectory.resolve(source).toFile())){
            zipOutputStream.putNextEntry(new ZipEntry(pathToString(destination)));
            IOUtils.copy(in, zipOutputStream);
          }
        }
        zipOutputStream.closeEntry();
      }

      zipOutputStream.finish();

    } catch (Exception e) {
      throw e;
    }
  }

  public static Path getResponsePath(Path path) {
    return path.getParent().resolve(path.getFileName().toString() + Constants.RESPONSE_SUFFIX);
  }

  public static Envelope loadAndExtract(Path baseDirectory, Path dataPath) throws Exception {
    Envelope envelope = new Envelope(baseDirectory);

    if (!dataPath.isAbsolute()) {
      dataPath = baseDirectory.resolve(dataPath);
    }

    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(dataPath.toFile()), StandardCharsets.UTF_8)) {
      ZipEntry entry = null;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.getName().equals(MESSAGE_BUNDLE)) {
          envelope.messageBundle = MessageBundle.load(zipInputStream);
        } else {
          Path entryPath = baseDirectory.resolve(FILES.relativize(Paths.get(entry.getName())));
          if (entryPath.toFile().exists()) {
            deleteFiles(entryPath);
          }
          envelope.filePathList.add(baseDirectory.relativize(entryPath));
          if (entry.isDirectory()) {
            Files.createDirectories(entryPath);
          } else {
            Files.createDirectories(entryPath.getParent());
            try (OutputStream out = new FileOutputStream(entryPath.toFile())){
              IOUtils.copy(zipInputStream, out);
            }
          }
        }
      }
    }

    return envelope;
  }

  private static void deleteFiles(Path path) {
    if (Files.isDirectory(path)) {
      try (Stream<Path> files = Files.list(path)) {
        files.forEach(p -> deleteFiles(p));
      } catch (IOException e) {
        e.printStackTrace();
      }

      try {
        Files.delete(path);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      try {
        Files.delete(path);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void addPathsToSet(Set<Path> set, Path path) {
    Path absolutePath = baseDirectory.resolve(path);
    if (Files.isDirectory(baseDirectory.resolve(path))) {
      set.add(path.normalize());
      try (Stream<Path> files = Files.list(absolutePath)) {
        files.forEach(p -> addPathsToSet(set, baseDirectory.relativize(p)));
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      //TODO: handling symbolic link
      set.add(path.normalize());
    }
  }

  private static String pathToString(Path path) {
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

  private static ZipEntry createDirectoryZipEntry(Path path) {
    return new ZipEntry(pathToString(path) + '/');
  }
}
