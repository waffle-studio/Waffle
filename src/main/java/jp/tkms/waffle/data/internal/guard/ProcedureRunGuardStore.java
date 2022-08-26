package jp.tkms.waffle.data.internal.guard;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.util.WaffleId;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ProcedureRunGuardStore {

  private static final String GUARD = "GUARD";
  private LinkedHashMap<WaffleId, ProcedureRunGuard> guardMap;
  private LinkedHashMap<String, ArrayList<ProcedureRunGuard>> guardListMapByWorkspace;
  private LinkedHashMap<String, ArrayList<ProcedureRunGuard>> guardListMapByRun;

  protected ProcedureRunGuardStore() {
    guardMap = new LinkedHashMap<>();
    guardListMapByWorkspace = new LinkedHashMap<>();
    guardListMapByRun = new LinkedHashMap<>();
  }

  public ProcedureRunGuard getGuard(WaffleId id) {
    synchronized (guardMap) {
      return guardMap.get(id);
    }
  }

  public ArrayList<ProcedureRunGuard> getList() {
    synchronized (guardMap) {
      return new ArrayList<>(guardMap.values());
    }
  }

  public ArrayList<ProcedureRunGuard> getList(Workspace workspace) {
    ArrayList<ProcedureRunGuard> list = new ArrayList<>();
    ArrayList originalList = guardListMapByWorkspace.get(workspace.getLocalPath().toString());
    if (originalList != null) {
      list.addAll(originalList);
    }
    return list;
  }

  public ArrayList<ProcedureRunGuard> getList(AbstractRun target) {
    ArrayList<ProcedureRunGuard> list = new ArrayList<>();
    ArrayList originalList = guardListMapByRun.get(Constants.WORK_DIR.relativize(target.getPath()).normalize().toString());
    if (originalList != null) {
      list.addAll(originalList);
    }
    return list;
  }

  public boolean contains(WaffleId id) {
    synchronized (guardMap) {
      return guardMap.containsKey(id);
    }
  }

  public void register(ProcedureRunGuard guard) {
    synchronized (guardMap) {
      guardMap.put(guard.getId(), guard);
      ArrayList<ProcedureRunGuard> list = null;

      String workspaceName = guard.getWorkspace().getLocalPath().toString();
      list = guardListMapByWorkspace.get(workspaceName);
      if (list == null) {
        list = new ArrayList<>();
        guardListMapByWorkspace.put(workspaceName, list);
      }
      list.add(guard);

      list = guardListMapByRun.get(guard.getTargetRunPath());
      if (list == null) {
        list = new ArrayList<>();
        guardListMapByRun.put(guard.getTargetRunPath(), list);
      }
      list.add(guard);
    }
  }

  public void remove(WaffleId id) {
    if (id == null) {
      return;
    }
    synchronized (guardMap) {
      ProcedureRunGuard removedStep = guardMap.remove(id);
      if (removedStep != null) {
        ArrayList<ProcedureRunGuard> list = null;
        list = guardListMapByWorkspace.get(removedStep.getWorkspace().getLocalPath().toString());
        if (list != null) {
          list.remove(removedStep);
        }

        list = guardListMapByRun.get(removedStep.getTargetRunPath());
        if (list != null) {
          list.remove(removedStep);
        }

        try {
          Path filePath = removedStep.getPropertyStorePath();
          Files.delete(filePath);
          tryDeleteEmptyDirectory(filePath.getParent());
        } catch (IOException e) {
          //NOP
        }
      }
    }
  }

  public void remove(ProcedureRunGuard guard) {
    remove(guard.getId());
  }

  public static ProcedureRunGuardStore load() {
    InfoLogMessage.issue("Loading the snapshot of guard store");

    ProcedureRunGuardStore store = new ProcedureRunGuardStore();
    Path directory = getDirectoryPath();

    try {
      Files.createDirectories(directory);
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    ArrayList<String> nameList = new ArrayList<>();
    HashMap<String, Path> pathMap = new HashMap<>();

    try (Stream<Path> stream = Files.walk(directory)) {
      stream.forEach(path -> {
        String name = path.getFileName().toString().toLowerCase();
        if (Files.isRegularFile(path)) {
          if (name.endsWith(".json")) {
            nameList.add(name);
            pathMap.put(name, path);
          }
        }
      });
    } catch (IOException e) {
      WarnLogMessage.issue(e);
    }

    nameList.sort(Comparator.naturalOrder());

    int fileCount = 0;
    for (String name : nameList) {
      Path jsonPath = pathMap.get(name);
      try {
        if (jsonPath != null) {
          store.register(ProcedureRunGuard.factory(jsonPath));
          fileCount += 1;
        }
      } catch (Exception e) {
        try {
          Files.delete(jsonPath);
          tryDeleteEmptyDirectory(jsonPath.getParent());
        } catch (IOException ioException) {
          // NOP
        }
        WarnLogMessage.issue(jsonPath.toString() + " is broken : " + e.getMessage());
      }
    }

    InfoLogMessage.issue(fileCount + " guards loaded");

    return store;
  }

  private static void loadDirectoryContentsTo(ArrayList<String> nameList, HashMap<String, Path> pathMap, File directory) {
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        loadDirectoryContentsTo(nameList, pathMap, file);
        tryDeleteEmptyDirectory(file.toPath());
      } else {
        nameList.add(file.getName());
        pathMap.put(file.getName(), file.toPath().toAbsolutePath());
      }
    }
  }

  private static void tryDeleteEmptyDirectory(Path directory) {
    if (directory.toAbsolutePath().normalize().equals(getDirectoryPath())) {
      return;
    }
    if (Files.isDirectory(directory)) {
      if (directory.toFile().listFiles().length <= 0) {
        try {
          Files.delete(directory);
          tryDeleteEmptyDirectory(directory.getParent());
        } catch (IOException e) {
          // NOP
        }
      }
    } else {
      tryDeleteEmptyDirectory(directory.getParent());
    }
  }

  public static Path getDirectoryPath() {
    return Constants.WORK_DIR.resolve(Constants.DOT_INTERNAL).resolve(GUARD);
  }
}
