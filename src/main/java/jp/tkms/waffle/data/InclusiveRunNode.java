package jp.tkms.waffle.data;

import jp.tkms.waffle.data.log.ErrorLogMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class InclusiveRunNode extends RunNode {
  public static final String KEY_INCLUSIVE = "INCLUSIVE";

  public InclusiveRunNode(Workspace workspace, Path path) {
    super(workspace, path);
    Path flagPath = getDirectoryPath(workspace, path).resolve(KEY_INCLUSIVE);
    if (! Files.exists(flagPath)) {
      try {
        flagPath.toFile().createNewFile();
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }
}
