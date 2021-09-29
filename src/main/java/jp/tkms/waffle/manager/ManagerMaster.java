package jp.tkms.waffle.manager;

import jp.tkms.waffle.data.project.workspace.Workspace;

import java.util.HashMap;
import java.util.Map;

public class ManagerMaster {
  private static final Map<String, Manager> managerMap = new HashMap<>();

  public static void update(Workspace workspace) {
    Manager manager = managerMap.get(workspace.getLocalDirectoryPath().toString());
    if (manager != null) {
      manager.update();
    }
  }
}
