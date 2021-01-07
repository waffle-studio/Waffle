package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.Project;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public interface InternalHashedLinkData extends InternalHashedData {
  default String getDataId(Path path) {
    return getDataId(getProject(), getInternalDataGroup(), path);
  }

  @Override
  default Path getDataDirectory(UUID id) {
    return getDataPath(getProject(), getInternalDataGroup(), id.toString());
  }

  static String getDataId(Project project, String group, Path path) {
    String id = null;
    Path localPath = InternalHashedData.getLocalPath(path);
    Path idFilePath = Constants.WORK_DIR.resolve(localPath.resolve(".id"));
    if (Files.exists(idFilePath)) {
      try {
        id = Files.readString(idFilePath);
      } catch (IOException e) {
      }
    }

    if (id == null) {
      id = UUID.randomUUID().toString();
      try {
        FileWriter filewriter = new FileWriter(idFilePath.toFile());
        filewriter.write(id);
        filewriter.close();
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    updateDataPath(project, group, id, localPath);

    return id;
  }

  static void resetDataId(Path path) {
    Path idFilePath = Constants.WORK_DIR.resolve(InternalHashedData.getLocalPath(path).resolve(".id"));
    if (Files.exists(idFilePath)) {
      idFilePath.toFile().delete();
    }
  }

  static void updateDataPath(Project project, String group, String id, Path path) {
    Path hashedDirectoryPath = InternalHashedData.getDataDirectory(project, group, UUID.fromString(id));
    Path pathFilePath = hashedDirectoryPath.resolve("path");
    try {
      FileWriter filewriter = new FileWriter(pathFilePath.toFile());
      filewriter.write(InternalHashedData.getLocalPath(path).toString());
      filewriter.close();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
  }

  static Path getDataPath(Project project, String group, String id) {
    Path pathFilePath = InternalHashedData.getHashedDirectoryPath(project, group, UUID.fromString(id)).resolve("path");
    if (Files.exists(pathFilePath)) {
      try {
        return Constants.WORK_DIR.resolve(Files.readString(pathFilePath));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }
    return null;
  }
}
