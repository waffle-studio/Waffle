package jp.tkms.waffle.data.internal.step;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.util.WaffleId;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class ProcedureRunGuardStore {

  private static final String GUARD = "GUARD";
  private LinkedHashMap<WaffleId, ProcedureRunGuard> guardMap;
  private LinkedHashMap<String, ArrayList<ProcedureRunGuard>> workspaceGuardListMap;
  private LinkedHashMap<String, ArrayList<ProcedureRunGuard>> abstractRunGuardListMap;

  protected ProcedureRunGuardStore() {
    guardMap = new LinkedHashMap<>();
    workspaceGuardListMap = new LinkedHashMap<>();
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
    synchronized (guardMap) {
      ArrayList list = workspaceGuardListMap.get(workspace.getLocalPath().toString());
      if (list == null) {
        list = new ArrayList();
        workspaceGuardListMap.put(workspace.getName(), list);
      }
      return list;
    }
  }

  public boolean contains(WaffleId id) {
    synchronized (guardMap) {
      return guardMap.containsKey(id);
    }
  }

  public void register(ProcedureRunGuard guard) {
    synchronized (guardMap) {
      guardMap.put(guard.getId(), guard);
      getList(guard.getWorkspace()).add(guard);
    }
  }

  public void remove(WaffleId id) {
    if (id == null) {
      return;
    }
    synchronized (guardMap) {
      ProcedureRunGuard removedStep = guardMap.remove(id);
      if (removedStep != null) {
        getList(removedStep.getWorkspace()).remove(removedStep);
      }
    }
  }

  public static ProcedureRunGuardStore load() {
    InfoLogMessage.issue("Loading the snapshot of step store");

    ProcedureRunGuardStore store = new ProcedureRunGuardStore();
    Path directory = getDirectoryPath();

    try {
      Files.createDirectories(directory);
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    int fileCount = 0;
    for (File projectDir : directory.toFile().listFiles(File::isDirectory)) {
      //Project project = Project.getInstance(projectDir.getName());
      for (File workspaceDir : projectDir.listFiles(File::isDirectory)) {
        //Workspace workspace = Workspace.getInstance(project, workspaceDir.getName());

        for (Object object : Arrays.stream(workspaceDir.listFiles()).sorted().toArray()) {
          File file = (File) object;

          try {
            Path jsonPath = file.toPath();
            if (jsonPath != null) {
              JSONObject jsonObject = new JSONObject(Files.readString(jsonPath));
              WaffleId id = WaffleId.valueOf(jsonObject.getLong(ProcedureRunGuard.KEY_ID));
              Path path = Paths.get(jsonObject.getString(ProcedureRunGuard.KEY_PATH));
              String projectName = jsonObject.getString(ProcedureRunGuard.KEY_PROJECT);
              String workspaceName = jsonObject.getString(ProcedureRunGuard.KEY_WORKSPACE);
              String targetRunPath = jsonObject.getString(ProcedureRunGuard.KEY_TARGET);
              store.register(new ProcedureRunGuard(id, path, projectName, workspaceName, targetRunPath));
              fileCount += 1;
            }
          } catch (Exception e) {
            file.delete();
            WarnLogMessage.issue(file.toPath().toString() + " is broken : " + e.getMessage());
          }
        }

        if (workspaceDir.listFiles().length <= 0) {
          workspaceDir.delete();
        }
      }

      if (projectDir.listFiles().length <= 0) {
        projectDir.delete();
      }
    }

    InfoLogMessage.issue(fileCount + " job loaded");

    return store;
  }

  public static Path getDirectoryPath() {
    return Constants.WORK_DIR.resolve(Constants.DOT_INTERNAL).resolve(GUARD);
  }
}
