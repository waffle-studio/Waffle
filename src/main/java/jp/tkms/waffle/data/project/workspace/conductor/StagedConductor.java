package jp.tkms.waffle.data.project.workspace.conductor;

import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.workspace.Workspace;

public class StagedConductor extends Conductor {
  Workspace workspace;

  public StagedConductor(Workspace workspace, String name) {
    super(workspace.getProject(), name);
    this.workspace = workspace;
  }
}
