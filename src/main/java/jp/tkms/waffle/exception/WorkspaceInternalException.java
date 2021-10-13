package jp.tkms.waffle.exception;

import jp.tkms.waffle.data.project.workspace.Workspace;

public class WorkspaceInternalException extends WaffleException {
  Workspace workspace;

  public WorkspaceInternalException(Workspace workspace, Throwable e) {
    super(e);
    this.workspace = workspace;
  }

  public WorkspaceInternalException(Workspace workspace, String message) {
    this.workspace = workspace;
    setMessage(message);
  }

  public Workspace getWorkspace() {
    return workspace;
  }
}
