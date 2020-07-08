package jp.tkms.waffle.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ParallelRunNode extends RunNode {
  public static final String KEY_PARALLEL = "PARALLEL";

  public ParallelRunNode(Project project, Path path) {
    super(project, path);
    Path flagPath = path.resolve(KEY_PARALLEL);
    if (! Files.exists(flagPath)) {
      try {
        flagPath.toFile().createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
