package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Registry;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedConductor;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;
import jp.tkms.waffle.data.util.ComputerState;
import jp.tkms.waffle.data.util.InstanceCache;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.script.ScriptProcessor;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class RunCapsule extends AbstractRun {
  public static final String CAPSULE = "CAPSULE";
  public static final String JSON_FILE = CAPSULE + Constants.EXT_JSON;

  private static final InstanceCache<String, RunCapsule> instanceCache = new InstanceCache<>();

  public RunCapsule(Workspace workspace, AbstractRun parent, Path path) {
    super(workspace, parent, path);
    instanceCache.put(getLocalDirectoryPath().toString(), this);
  }

  @Override
  public void start() {
  }

  @Override
  public void finish() {
  }

  @Override
  protected Path getVariablesStorePath() {
    return getParent().getVariablesStorePath();
  }

  @Override
  public AbstractRun getResponsible() {
    return getParent().getResponsible();
  }

  @Override
  public State getState() {
    State state = State.None;
    for (AbstractRun run : getList()) {
      state = state.getHigh(run.getState());
    }
    return state;
  }

  @Override
  public void registerChildActiveRun(AbstractRun abstractRun) {
    getParent().registerChildActiveRun(abstractRun);
  }

  @Override
  public void registerChildRun(AbstractRun abstractRun) {
    getParent().registerChildRun(abstractRun);
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(JSON_FILE);
  }

  public static RunCapsule create(AbstractRun parent, String expectedName) {
    String name = expectedName;
    RunCapsule instance = new RunCapsule(parent.getWorkspace(), parent, parent.getDirectoryPath().resolve(name));
    return instance;
  }

  public static RunCapsule getInstance(Workspace workspace, String localPathString) {
    RunCapsule instance = instanceCache.get(localPathString);
    if (instance != null) {
      return instance;
    }

    Path jsonPath = Constants.WORK_DIR.resolve(localPathString).resolve(JSON_FILE);

    if (Files.exists(jsonPath)) {
      try {
        JSONObject jsonObject = new JSONObject(StringFileUtil.read(jsonPath));
        AbstractRun parent = AbstractRun.getInstance(workspace, jsonObject.getString(KEY_PARENT_RUN));
        instance = new RunCapsule(workspace, parent, jsonPath.getParent());
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
    }

    return instance;
  }
}
