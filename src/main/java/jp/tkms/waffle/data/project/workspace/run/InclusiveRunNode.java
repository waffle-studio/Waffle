package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.data.project.Project;

import java.nio.file.Path;

public class InclusiveRunNode extends RunNode {

  public InclusiveRunNode(Project project, Path path) {
    super(project, path);
  }
}
