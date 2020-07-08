package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.State;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class SimulatorRunNode extends RunNode {
  protected static final String KEY_SIMULATOR = ".SIMULATOR";

  public SimulatorRunNode(Project project, Path path) {
    super(project, path);
    Path flagPath = path.resolve(KEY_SIMULATOR);
    if (! Files.exists(flagPath)) {
      try {
        flagPath.toFile().createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void updateState(State prev, State next) {
    propagateState(prev, next);
  }

  @Override
  public State getState() {
    SimulatorRun run = SimulatorRun.getInstance(project, getId());
    if (run == null) {
      return State.None;
    }
    return run.getState();
  }
}
