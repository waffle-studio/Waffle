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
import jp.tkms.waffle.data.util.*;
import jp.tkms.waffle.exception.ChildProcedureNotFoundException;
import jp.tkms.waffle.script.ScriptProcessor;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ProcedureRun extends AbstractRun {
  //public static final String PROCEDURE_RUN = "PROCEDURE_RUN";
  public static final String PROCEDURE_RUN = ".PROCEDURE_RUN";
  public static final String KEY_CONDUCTOR = "conductor";
  public static final String KEY_PROCEDURE_NAME = "procedure_name";
  public static final String KEY_WORKING_DIRECTORY = "working_directory";

  private ArchivedConductor conductor;
  private String procedureName;
  private Registry registry;
  private Path workingDirectory;

  private static final InstanceCache<String, ProcedureRun> instanceCache = new InstanceCache<>();

  public ProcedureRun(Workspace workspace, ConductorRun parentConductorRun, Path path, Path workingDirectory, ArchivedConductor conductor, String procedureName) {
    super(workspace, parentConductorRun, path);
    instanceCache.put(getPath().toString(), this);
    setConductor(conductor);
    setProcedureName(procedureName);
    setWorkingDirectory(workingDirectory);
    registry = new Registry(getWorkspace());
  }

  public static String debugReport() {
    return ProcedureRun.class.getSimpleName() + ": instanceCacheSize=" + instanceCache.size();
  }

  public void setWorkingDirectory(Path workingDirectory) {
    this.workingDirectory = workingDirectory;
    setToProperty(KEY_WORKING_DIRECTORY, workingDirectory.toString());
  }

  public Path getWorkingDirectory() {
    if (this.workingDirectory == null) {
      this.workingDirectory = Paths.get(getStringFromProperty(KEY_WORKING_DIRECTORY));
    }
    return this.workingDirectory;
  }

  public void addReferable(AbstractRun run) {
  }

  public ArrayList<AbstractRun> getReferable() {
    ArrayList<AbstractRun> clonedList = new ArrayList<>();
    return clonedList;
  }

  @Override
  public void start() {
    started();
    setState(State.Running);

    if (conductor != null) {
      if (Conductor.MAIN_PROCEDURE_ALIAS.equals(procedureName)) {
        ScriptProcessor.getProcessor(conductor.getMainProcedureScriptPath()).processProcedure(this, getReferable(), conductor.getMainProcedureScript());
      } else {
        try {
          ScriptProcessor.getProcessor(conductor.getChildProcedureScriptPath(procedureName)).processProcedure(this, getReferable(), conductor.getChildProcedureScript(procedureName));
        } catch (ChildProcedureNotFoundException e) {
          //NOOP
        }
      }
    }
    /*
    if (getChildrenRunSize() <= 0) {
      reportFinishedRun(null);
    }
     */
  }

  /*
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
   */

  @Override
  public void finish() {
    setState(State.Finalizing);
    /*
    processFinalizers();
    getResponsible().reportFinishedRun(this);

     */
    setState(State.Finished);
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getId() {
    return null;
  }

  @Override
  public Path getPropertyStorePath() {
    return getPath();
  }

  public Registry getRegistry() {
    return registry;
  }

  private static Path getNewProcedureRunPath(ConductorRun conductorRun, String procedureName) {
    Path path = conductorRun.getLocalDirectoryPath();
    path = path.resolve(PROCEDURE_RUN).resolve(procedureName);
    String id = WaffleId.newId().toString();
    path = path.resolve(id.substring(0, 8)).resolve(id.substring(8, 10)).resolve(id.substring(10, 12)).resolve(id);
    return path;
  }

  public static ProcedureRun create(ConductorRun parent, ArchivedConductor conductor, String procedureName) {
    ProcedureRun instance = new ProcedureRun(parent.getWorkspace(), parent, getNewProcedureRunPath(parent, procedureName), parent.getWorkspace().getLocalDirectoryPath(), conductor, procedureName);
    instance.setState(State.Created);
    return instance;
  }

  public static ProcedureRun create(ProcedureRun parent, ArchivedConductor conductor, String procedureName) {
    ProcedureRun instance = new ProcedureRun(parent.getWorkspace(), parent.getParentConductorRun(), getNewProcedureRunPath(parent.getParentConductorRun(), procedureName), parent.getWorkingDirectory(), conductor, procedureName);
    instance.setState(State.Created);
    return instance;
  }

  public static Path toJsonPath(Path path) {
    return path.getParent().resolve(path.getFileName().toString() + Constants.EXT_JSON);
  }

  public static ProcedureRun getInstance(Workspace workspace, String localPathString) {
    ProcedureRun instance = instanceCache.get(localPathString);
    if (instance != null) {
      return instance;
    }

    Path jsonPath = Constants.WORK_DIR.resolve(localPathString);

    if (Files.exists(jsonPath)) {
      try {
        JSONObject jsonObject = new JSONObject(StringFileUtil.read(jsonPath));
        ConductorRun parent = ConductorRun.getInstance(workspace, jsonObject.getString(KEY_PARENT_CONDUCTOR_RUN));
        ArchivedConductor conductor = null;
        if (jsonObject.keySet().contains(KEY_CONDUCTOR)) {
          conductor = ArchivedConductor.getInstance(workspace, jsonObject.getString(KEY_CONDUCTOR));
        }
        String procedureName = null;
        if (jsonObject.keySet().contains(KEY_PROCEDURE_NAME)) {
          procedureName = jsonObject.getString(KEY_PROCEDURE_NAME);
        }
        Path workingDirectory = null;
        if (jsonObject.keySet().contains(KEY_WORKING_DIRECTORY)) {
          workingDirectory = Paths.get(jsonObject.getString(KEY_WORKING_DIRECTORY));
        }
        instance = new ProcedureRun(workspace, parent, Paths.get(localPathString), workingDirectory, conductor, procedureName);
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
    }

    return instance;
  }

  /*
  public static ProcedureRun getTestRunProcedureRun(ArchivedExecutable executable) {
    return create(ConductorRun.getTestRunConductorRun(executable), executable.getArchiveName(), null, null);
  }
   */

  private void setConductor(ArchivedConductor conductor) {
    this.conductor = conductor;
    if (conductor != null) {
      setToProperty(KEY_CONDUCTOR, conductor.getArchiveName());
    } else {
      removeFromProperty(KEY_CONDUCTOR);
    }
  }

  private void setProcedureName(String procedureName) {
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

  /*
  public SyncExecutableRun createSyncExecutableRun(String executableName, String computerName, String name) {
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

    SyncExecutableRun syncExecutableRun = SyncExecutableRun.create(this, name, executable, computer);
    transactionRunList.add(syncExecutableRun);

    return syncExecutableRun;
  }
   */

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
