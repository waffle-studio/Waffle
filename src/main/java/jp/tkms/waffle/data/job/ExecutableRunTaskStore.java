package jp.tkms.waffle.data.job;

import jp.tkms.waffle.Constants;

import java.nio.file.Path;

public class ExecutableRunTaskStore extends AbstractTaskStore<ExecutableRunJob> {
  public static final byte TYPE_CODE = 1;
  public static final String TASK = "TASK";

  public ExecutableRunTaskStore() {
    super();
  }

  public static ExecutableRunTaskStore load() {
    ExecutableRunTaskStore instance = new ExecutableRunTaskStore();
    load(instance, getDirectoryPath(), (i, p, c)-> new ExecutableRunJob(i, p, c));
    return instance;
  }

  public static Path getDirectoryPath() {
    return Constants.WORK_DIR.resolve(Constants.DOT_INTERNAL).resolve(TASK);
  }
}
