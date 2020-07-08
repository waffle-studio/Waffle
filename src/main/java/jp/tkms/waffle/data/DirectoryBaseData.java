package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.Sql;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public abstract class DirectoryBaseData extends PropertyFileData implements DataDirectory {

  private int rowid;

  public DirectoryBaseData() {}

  public DirectoryBaseData(Class clazz, Path path) {
    this.name = path.getFileName().toString();
    this.id = DataId.getInstance(clazz, path).getUuid();
  }

  public void replace(Path path) {
    int count = 1;
    while (Files.exists(path)) {
      path = path.getParent().resolve(path.getFileName().toString() + '_' + count++);
      //name = (name.length() > 0 ? "_" : "") + UUID.randomUUID().toString().replaceFirst("-.*$", "");
    }
    Path localPath = Constants.WORK_DIR.relativize(path.toAbsolutePath());

    if (Files.exists(getDirectoryPath())) {
      try {
        Files.move(getDirectoryPath(), path);
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    DataId dataId = DataId.getInstance(getId());
    dataId.setDirectory(localPath);
  }

  static public void resetId(Class clazz, Path path) {
    DataId.getInstance(clazz, path).resetId();
  }

  static String getName(String id) {
    DataId dataId = DataId.getInstance(id);
    if (dataId != null) {
      return dataId.getPath().getFileName().toString();
    }

    return null;
  }

  static Path getDirectoryPath(String id) {
    DataId dataId = DataId.getInstance(id);
    if (dataId != null) {
      return dataId.getPath();
    }

    return null;
  }
}
