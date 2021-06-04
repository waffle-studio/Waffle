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
import jp.tkms.waffle.exception.ChildProcedureNotFoundException;
import jp.tkms.waffle.script.ScriptProcessor;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ProcedureRun extends AbstractRun {
  public static final String PROCEDURE_RUN = "PROCEDURE_RUN";
  public static final String JSON_FILE = PROCEDURE_RUN + Constants.EXT_JSON;
  public static final String KEY_CONDUCTOR = "conductor";
  public static final String KEY_PROCEDURE_NAME = "procedure_name";

  private ArchivedConductor conductor;
  private String procedureName;
  private Registry registry;

  private static final InstanceCache<String, ProcedureRun> instanceCache = new InstanceCache<>();

  public ProcedureRun(Workspace workspace, AbstractRun parent, Path path, ArchivedConductor conductor, String procedureName) {
    super(workspace, parent, path);
    instanceCache.put(getLocalDirectoryPath().toString(), this);
    setConductor(conductor);
    setProcedureName(procedureName);
    registry = new Registry(getWorkspace());
  }

  public static String debugReport() {
    return ProcedureRun.class.getSimpleName() + ": instanceCacheSize=" + instanceCache.size();
  }

  @Override
  public void start() {
    startFinalizer(ScriptProcessor.ProcedureMode.START_OR_FINISHED_ALL, null);
  }

  public void startFinalizer(ScriptProcessor.ProcedureMode mode, AbstractRun caller) {
    started();
    setState(State.Running);
    getResponsible().registerChildActiveRun(this);
    if (caller == null) {
      caller = this;
    }
    if (conductor != null) {
      if (Conductor.MAIN_PROCEDURE_ALIAS.equals(procedureName)) {
        ScriptProcessor.getProcessor(conductor.getMainProcedureScriptPath()).processProcedure(this, mode, caller, conductor.getMainProcedureScript());
      } else {
        try {
          ScriptProcessor.getProcessor(conductor.getChildProcedureScriptPath(procedureName)).processProcedure(this, mode, caller, conductor.getChildProcedureScript(procedureName));
        } catch (ChildProcedureNotFoundException e) {
          //NOOP
        }
      }
    }
    if (getChildrenRunSize() <= 0) {
      reportFinishedRun(null);
    }
  }

  public void startHandler(ScriptProcessor.ProcedureMode mode, AbstractRun caller, ArrayList<Object> arguments) {
    if (caller == null) {
      caller = this;
    }
    if (conductor != null) {
      if (Conductor.MAIN_PROCEDURE_ALIAS.equals(procedureName)) {
        ScriptProcessor.getProcessor(conductor.getMainProcedureScriptPath()).processProcedure(this, mode, caller, conductor.getMainProcedureScript(), arguments);
      } else {
        try {
          ScriptProcessor.getProcessor(conductor.getChildProcedureScriptPath(procedureName)).processProcedure(this, mode, caller, conductor.getChildProcedureScript(procedureName), arguments);
        } catch (ChildProcedureNotFoundException e) {
          //NOOP
        }
      }
    }
  }

  @Override
  public void finish() {
    setState(State.Finalizing);
    processFinalizers();
    getResponsible().reportFinishedRun(this);
    setState(State.Finished);
  }

  @Override
  protected Path getVariablesStorePath() {
    return getParent().getVariablesStorePath();
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(JSON_FILE);
  }

  public Registry getRegistry() {
    return registry;
  }

  public static ProcedureRun create(AbstractRun parent, String expectedName, ArchivedConductor conductor, String procedureName) {
    String name = parent.generateUniqueFileName(expectedName);
    ProcedureRun instance = new ProcedureRun(parent.getWorkspace(), parent, parent.getDirectoryPath().resolve(name), conductor, procedureName);
    instance.setState(State.Created);
    instance.getParent().registerChildRun(instance);
    instance.updateResponsible();
    return instance;
  }

  public static ProcedureRun getInstance(Workspace workspace, String localPathString) {
    ProcedureRun instance = instanceCache.get(localPathString);
    if (instance != null) {
      return instance;
    }

    Path jsonPath = Constants.WORK_DIR.resolve(localPathString).resolve(JSON_FILE);

    if (Files.exists(jsonPath)) {
      try {
        JSONObject jsonObject = new JSONObject(StringFileUtil.read(jsonPath));
        AbstractRun parent = AbstractRun.getInstance(workspace, jsonObject.getString(KEY_PARENT_RUN));
        ArchivedConductor conductor = null;
        if (jsonObject.keySet().contains(KEY_CONDUCTOR)) {
          conductor = ArchivedConductor.getInstance(workspace, jsonObject.getString(KEY_CONDUCTOR));
        }
        String procedureName = null;
        if (jsonObject.keySet().contains(KEY_PROCEDURE_NAME)) {
          procedureName = jsonObject.getString(KEY_PROCEDURE_NAME);
        }
        instance = new ProcedureRun(workspace, parent, jsonPath.getParent(), conductor, procedureName);
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
    }

    return instance;
  }

  public static ProcedureRun getTestRunProcedureRun(ArchivedExecutable executable) {
    return create(ConductorRun.getTestRunConductorRun(executable), executable.getArchiveName(), null, null);
  }

  public void setConductor(ArchivedConductor conductor) {
    this.conductor = conductor;
    if (conductor != null) {
      setToProperty(KEY_CONDUCTOR, conductor.getArchiveName());
    } else {
      removeFromProperty(KEY_CONDUCTOR);
    }
  }

  public void setProcedureName(String procedureName) {
    this.procedureName = procedureName;
    if (procedureName != null) {
      setToProperty(KEY_PROCEDURE_NAME, procedureName);
    } else {
      removeFromProperty(KEY_PROCEDURE_NAME);
    }
  }

  private ArrayList<AbstractRun> transactionRunList = new ArrayList<>();

  public ConductorRun createConductorRun(String conductorName, String name) {
    Conductor conductor = Conductor.find(getProject(), conductorName);
    if (conductor == null) {
      throw new RuntimeException("Conductor\"(" + conductorName + "\") is not found");
    }

    ConductorRun conductorRun = ConductorRun.create(this, conductor, name);
    transactionRunList.add(conductorRun);

    return conductorRun;
  }

  public ExecutableRun createExecutableRun(String executableName, String computerName, String name) {
    Executable executable = Executable.getInstance(getProject(), executableName);
    if (executable == null) {
      throw new RuntimeException("Executable(\"" + executableName + "\") is not found");
    }
    Computer computer = Computer.find(computerName);
    if (computer == null) {
      throw new RuntimeException("Computer(\"" + computerName + "\") is not found");
    }
    //computer.update();
    if (! computer.getState().equals(ComputerState.Viable)) {
      throw new RuntimeException("Computer(\"" + computerName + "\") is not viable");
    }

    ExecutableRun executableRun = ExecutableRun.create(this, name, executable, computer);
    transactionRunList.add(executableRun);

    return executableRun;
  }

  public void commit() {
    /*
    //TODO: do refactor
    if (conductorTemplate != null) {
      RubyScript.process((container) -> {
        String script = listenerTemplate.getScript();
        try {
          container.runScriptlet(RubyConductor.getListenerTemplateScript());
          container.runScriptlet(script);
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_conductor_template_script", this, conductorTemplate);
        } catch (Exception e) {
          WarnLogMessage.issue(e);
          getRunNode().appendErrorNote(e.getMessage());
        }
      });
    } else if (listenerTemplate != null) {
      RubyScript.process((container) -> {
        String script = listenerTemplate.getScript();
        try {
          container.runScriptlet(RubyConductor.getListenerTemplateScript());
          container.runScriptlet(script);
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_listener_template_script", this, this);
        } catch (Exception e) {
          WarnLogMessage.issue(e);
          getRunNode().appendErrorNote(e.getMessage());
        }
      });
    }

    if (transactionRunList.size() > 1) {
      getRunNode().switchToParallel();
    }
     */

    for (AbstractRun createdRun : transactionRunList) {
      if (! createdRun.isStarted()) {
        createdRun.start();
      }
    }

    transactionRunList.clear();
  }
}
