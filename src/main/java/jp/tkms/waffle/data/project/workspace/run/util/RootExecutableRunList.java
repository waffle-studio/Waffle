package jp.tkms.waffle.data.project.workspace.run.util;

import jp.tkms.waffle.data.project.workspace.Workspace;

public class RootExecutableRunList extends AbstractExecutableRunList {

  public RootExecutableRunList(Workspace workspace) {
    super(workspace);
  }

  @Override
  protected void initProcess() {
  }

  @Override
  protected AbstractExecutableRunList createEmptyList() {
    return new RootExecutableRunList(getWorkspace());
  }

  @Override
  public FilteredExecutableRunList filterByTag(String tag) {
    return null;
  }
}
