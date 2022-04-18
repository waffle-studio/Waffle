package jp.tkms.waffle.sub.servant.message.request;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ChangePermissionMessage extends AbstractRequestMessage {
  String path;
  String permission;

  public ChangePermissionMessage() {}

  public ChangePermissionMessage(Path path, String permission) {
    this.path = path.toString();
    this.permission = permission;
  }

  public Path getPath() {
    return Paths.get(path);
  }

  public String getPermission() {
    return permission;
  }
}
