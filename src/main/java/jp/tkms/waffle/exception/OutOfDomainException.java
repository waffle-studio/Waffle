package jp.tkms.waffle.exception;

import jp.tkms.waffle.data.project.workspace.Workspace;

public class OutOfDomainException extends WorkspaceInternalException {
  public OutOfDomainException(Workspace workspace, String message) {
    super(workspace, message);
  }
}
