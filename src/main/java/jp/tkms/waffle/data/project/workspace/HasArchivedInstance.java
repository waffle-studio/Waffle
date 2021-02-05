package jp.tkms.waffle.data.project.workspace;

import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.util.WaffleId;

public interface HasArchivedInstance<A> extends DataDirectory {
  String ARCHIVE_ID = ".ARCHIVE_ID";

  A getArchivedInstance();

  default WaffleId getArchiveId() {
    return WaffleId.valueOf(getFileContents(ARCHIVE_ID));
  }
}
