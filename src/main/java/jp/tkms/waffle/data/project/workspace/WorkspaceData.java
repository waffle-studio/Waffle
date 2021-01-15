package jp.tkms.waffle.data.project.workspace;

import jp.tkms.waffle.data.project.ProjectData;

public class WorkspaceData extends ProjectData {
  private Workspace workspace;

  public WorkspaceData(Workspace workspace) {
    super(workspace.getProject());
    this.workspace = workspace;
  }

  public Workspace getWorkspace() {
    return workspace;
  }
}
