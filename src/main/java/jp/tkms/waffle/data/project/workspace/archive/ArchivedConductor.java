package jp.tkms.waffle.data.project.workspace.archive;

import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.conductor.Conductor;

public class ArchivedConductor extends Conductor {
  public ArchivedConductor(Project project, String name) {
    super(project, name);
  }
}
