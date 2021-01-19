package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.util.State;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
