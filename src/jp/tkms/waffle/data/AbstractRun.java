package jp.tkms.waffle.data;

import java.util.UUID;

abstract public class AbstractRun extends ProjectData {
  public AbstractRun(Project project) {
    super(project);
  }


  public AbstractRun(Project project, UUID id, String name) {
    super(project, id, name);
  }
}
