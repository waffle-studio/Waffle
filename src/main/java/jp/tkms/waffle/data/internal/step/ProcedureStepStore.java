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

public class ProcedureStepStore {

  private static final String STEP = "STEP";
  private LinkedHashMap<WaffleId, ProcedureStep> stepMap;
  private LinkedHashMap<String, ArrayList<ProcedureStep>> workspaceStepListMap;

  protected ProcedureStepStore() {
    stepMap = new LinkedHashMap<>();
    workspaceStepListMap = new LinkedHashMap<>();
  }

  public ProcedureStep getStep(WaffleId id) {
    synchronized (stepMap) {
      return stepMap.get(id);
    }
  }

  public ArrayList<ProcedureStep> getList() {
    synchronized (stepMap) {
      return new ArrayList<>(stepMap.values());
    }
  }

  public ArrayList<ProcedureStep> getList(Workspace workspace) {
    synchronized (stepMap) {
      ArrayList list = workspaceStepListMap.get(workspace.getLocalDirectoryPath().toString());
      if (list == null) {
        list = new ArrayList();
        workspaceStepListMap.put(workspace.getName(), list);
      }
      return list;
    }
  }

  public boolean contains(WaffleId id) {
    synchronized (stepMap) {
      return stepMap.containsKey(id);
    }
  }

  public void register(ProcedureStep step) {
    synchronized (stepMap) {
      stepMap.put(step.getId(), step);
      getList(step.getWorkspace()).add(step);
    }
  }

  public void remove(WaffleId id) {
    if (id == null) {
      return;
    }
    synchronized (stepMap) {
      ProcedureStep removedStep = stepMap.remove(id);
      if (removedStep != null) {
        getList(removedStep.getWorkspace()).remove(removedStep);
      }
    }
  }

  protected static ProcedureStepStore load() {
    InfoLogMessage.issue("Loading the snapshot of step store");

    ProcedureStepStore store = new ProcedureStepStore();
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
              WaffleId id = WaffleId.valueOf(jsonObject.getLong(ProcedureStep.KEY_ID));
              Path path = Paths.get(jsonObject.getString(ProcedureStep.KEY_PATH));
              String projectName = jsonObject.getString(ProcedureStep.KEY_PROJECT);
              String workspaceName = jsonObject.getString(ProcedureStep.KEY_WORKSPACE);
              store.register(new ProcedureStep(id, path, projectName, workspaceName));
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
    return Constants.WORK_DIR.resolve(Constants.DOT_INTERNAL).resolve(STEP);
  }
}
