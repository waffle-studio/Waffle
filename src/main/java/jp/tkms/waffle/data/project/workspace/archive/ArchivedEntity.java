package jp.tkms.waffle.data.project.workspace.archive;

import jp.tkms.waffle.data.HasName;
import jp.tkms.waffle.data.util.WaffleId;

public interface ArchivedEntity extends HasName {
  WaffleId getId();

  default String getArchiveName() {
    return getArchiveName(getName(), getId());
  }

  static String getArchiveName(String name, WaffleId id) {
    return name + "-" + id.getReversedBase36Code();
  }
}
