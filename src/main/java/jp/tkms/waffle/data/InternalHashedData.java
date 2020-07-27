package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public interface InternalHashedData {
  Project getProject();
  String getInternalDataGroup();

  default Path getDataDirectory(UUID id) {
    return getDataDirectory(getProject(), getInternalDataGroup(), id);
  }

  static Path getDataDirectory(Project project, String group, UUID id) {
    Path hashedDirectoryPath = getHashedDirectoryPath(project, group, id);

    try {
      Files.createDirectories(hashedDirectoryPath);
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    return hashedDirectoryPath;
  }

  static Path getHashedDirectoryPath(Project project, String group, UUID id) {
    return project.getDirectoryPath().resolve(Constants.DOT_INTERNAL).resolve(group)
      .resolve(id.toString().substring(0, 4)).resolve(id.toString());
  }

  static Path getLocalPath(Path path) {
    if (path.isAbsolute()) {
      return Constants.WORK_DIR.relativize(path);
    }
    return path;
  }
}
