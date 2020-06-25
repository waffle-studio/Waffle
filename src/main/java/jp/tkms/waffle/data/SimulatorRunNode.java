package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.FileName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimulatorRunNode extends RunNode {
  protected static final String KEY_TMP = ".tmp";
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

  public InclusiveRunNode moveToVirtualNode() {
    String virtualNodeName = getDirectoryPath().getFileName().toString();
    RunNode parent = getParent();
    String name = FileName.removeRestrictedCharacters(getExpectedName());
    if (name.length() <= 0) {
      name = "_0";
    }
    rename(KEY_TMP);
    InclusiveRunNode node = parent.createInclusiveRunNode(virtualNodeName);
    replace(node.getDirectoryPath().resolve(name));
    return node;
  }
}
