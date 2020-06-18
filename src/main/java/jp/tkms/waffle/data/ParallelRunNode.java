package jp.tkms.waffle.data;

import java.nio.file.Path;

public class ParallelRunNode extends RunNode {
  public ParallelRunNode(Project project, Path path) {
    super(project, path);
  }
}
