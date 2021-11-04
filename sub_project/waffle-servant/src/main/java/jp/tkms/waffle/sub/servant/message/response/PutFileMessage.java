package jp.tkms.waffle.sub.servant.message.response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PutFileMessage extends AbstractResponseMessage {
  String path;
  byte[] contents;

  public PutFileMessage() { }

  public PutFileMessage(Path path, Path targetFilePath) throws IOException {
    this.path = path.normalize().toString();
    contents = Files.readAllBytes(targetFilePath);
  }

  public void putFile() {
    Path path = Paths.get(this.path);
    try {
      Files.createDirectories(path.getParent());
      Files.write(path, contents);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
