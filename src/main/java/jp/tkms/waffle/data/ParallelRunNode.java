package jp.tkms.waffle.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ParallelRunNode extends RunNode {
  public static final String KEY_PARALLEL = "PARALLEL";

  public ParallelRunNode(Workspace workspace, Path path) {
    super(workspace, path);
    Path flagPath = getDirectoryPath(workspace, path).resolve(KEY_PARALLEL);
    if (! Files.exists(flagPath)) {
      try {
        flagPath.toFile().createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
