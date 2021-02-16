package jp.tkms.waffle.data.job;

import jp.tkms.waffle.Constants;
import java.nio.file.Path;

public class SystemTaskStore extends AbstractTaskStore<SystemTaskJob> {
  public static final String SYSTEM_TASK = "SYSTEM_TASK";

  public SystemTaskStore() {
    super();
  }

  public static SystemTaskStore load() {
    SystemTaskStore instance = new SystemTaskStore();
    load(instance, getDirectoryPath(), (i, p, c)-> new SystemTaskJob(i, p, c));
    return instance;
  }

  public static Path getDirectoryPath() {
    return Constants.WORK_DIR.resolve(Constants.DOT_INTERNAL).resolve(SYSTEM_TASK);
  }
}
