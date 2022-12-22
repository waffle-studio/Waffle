package jp.tkms.waffle.data.project.workspace.run.util;

import jp.tkms.waffle.data.project.workspace.Workspace;

public class FilteredExecutableRunList extends AbstractExecutableRunList {

  protected FilteredExecutableRunList(Workspace workspace) {
    super(workspace);
  }

  @Override
  protected void initProcess() {
    // NOP
  }

  @Override
  protected void init() {
    // NOP
  }

  @Override
  protected AbstractExecutableRunList createEmptyList() {
    return new FilteredExecutableRunList(getWorkspace());
  }

  @Override
  public FilteredExecutableRunList filterByTag(String tag) {
    return null;
  }
}
