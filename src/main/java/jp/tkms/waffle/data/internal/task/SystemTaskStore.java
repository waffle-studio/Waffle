package jp.tkms.waffle.data.internal.task;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.internal.InternalFiles;

import java.nio.file.Path;

public class SystemTaskStore extends AbstractTaskStore<SystemTask> {
  public static final byte TYPE_CODE = 0;
  public static final String SYSTEM_TASK = "SYSTEM_TASK";

  @Override
  protected String getName() {
    return "system";
  }

  public SystemTaskStore() {
    super();
  }

  public static SystemTaskStore load() {
    SystemTaskStore instance = new SystemTaskStore();
    load(instance, getDirectoryPath(), (i, p, c)-> new SystemTask(i, p, c));
    return instance;
  }

  public static Path getDirectoryPath() {
    return InternalFiles.getPath(SYSTEM_TASK);
  }
}
