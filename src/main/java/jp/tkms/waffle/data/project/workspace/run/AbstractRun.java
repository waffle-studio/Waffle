package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.WorkspaceData;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedConductor;
import jp.tkms.waffle.data.util.ChildElementsArrayList;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.JSONWriter;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.script.ScriptProcessor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

abstract public class AbstractRun extends WorkspaceData implements DataDirectory, PropertyFile {
  public static final String RUN = "RUN";
  public static final String KEY_NOTE_TXT = "NOTE.txt";
  public static final String KEY_ERROR_NOTE_TXT = "ERROR_NOTE.txt";
  protected static final String KEY_CLASS = "class";
  protected static final String KEY_NORMAL_FINALIZER = "normal_finalizer";
  protected static final String KEY_FAULT_FINALIZER = "fault_finalizer";
  protected static final String KEY_APPEAL_HANDLER = "appeal_handler";
  protected static final String KEY_PARENT_RUN = "parent_run";
  protected static final String KEY_CHILDREN_RUN = "children_run";
  protected static final String KEY_ACTIVE_CHILDREN_RUN = "active_children_run";
  public static final String KEY_RESPONSIBLE_RUN = "responsible_run";
  protected static final String KEY_STATE = "state";
  protected static final String KEY_STARTED = "started";

  private Path path;
  private State state = null;
  private JSONArray callstack = null;
  private ConductorRun owner = null;
  private AbstractRun parent = null;
  private AbstractRun responsible = null;

  public abstract void start();
  public abstract void finish();
  protected abstract Path getVariablesStorePath();

  public AbstractRun(Workspace workspace, AbstractRun parent, Path path) {
    super(workspace);
    this.path = path;
    setToProperty(KEY_CLASS, getClass().getConstructors()[0].getDeclaringClass().getSimpleName());
    setParent(parent);
    if (parent == null && this instanceof ConductorRun) {
      this.owner = (ConductorRun) this;
    } else if (parent instanceof ConductorRun) {
      this.owner = (ConductorRun) parent;
    } else {
      if (parent != null) {
        this.owner = parent.getOwner();
      }
    }
  }

  public static AbstractRun getInstance(Workspace workspace, String localPathString) throws RunNotFoundException {
    if (workspace == null || localPathString == null) {
      return null;
    }

    Path basePath = Constants.WORK_DIR.resolve(localPathString);

    if (Files.exists(basePath.resolve(ConductorRun.JSON_FILE))) {
      return ConductorRun.getInstance(workspace, localPathString);
    } else if (Files.exists(basePath.resolve(ProcedureRun.JSON_FILE))) {
      return ProcedureRun.getInstance(workspace, localPathString);
    } else if (Files.exists(basePath.resolve(ExecutableRun.JSON_FILE))) {
      return ExecutableRun.getInstance(localPathString);
    } else if (Files.exists(basePath.resolve(RunCapsule.JSON_FILE))) {
      return RunCapsule.getInstance(workspace, localPathString);
    }

    return null;
  }

  public static ArrayList<AbstractRun> getList(Workspace workspace) {
    Path basePath = getBaseDirectoryPath(workspace);
    return new ChildElementsArrayList().getList(basePath, name -> {
      try {
        return getInstance(workspace, DataDirectory.toLocalDirectoryPath(basePath.resolve(name.toString())).toString());
      } catch (RunNotFoundException e) {
        ErrorLogMessage.issue(e);
      }
      return null;
    });
  }

  public ArrayList<AbstractRun> getList() {
    return new ChildElementsArrayList().getList(getDirectoryPath(), name -> {
      try {
        return getInstance(getWorkspace(), DataDirectory.toLocalDirectoryPath(getDirectoryPath().resolve(name.toString())).toString());
      } catch (RunNotFoundException e) {
        ErrorLogMessage.issue(e);
      }
      return null;
    });
  }

  public State getState() {
    if (state == null) {
      state = State.valueOf(getIntFromProperty(KEY_STATE, State.Created.ordinal()));
    }
    return state;
  }

  protected void setState(State state) {
    //System.out.println(state.name() + " : " + getId());
    this.state = state;
    setToProperty(KEY_STATE, state.ordinal());
  }

  public boolean isRunning() {
    State state = getState();
    return (state.equals(State.Created)
      || state.equals(State.Prepared)
      || state.equals(State.Submitted)
      || state.equals(State.Running)
      || state.equals(State.Finalizing)
    );
  }

  public AbstractRun getParent() {
    return parent;
  }

  protected void setResponsible(AbstractRun responsible) {
    this.responsible = responsible;
    setToProperty(KEY_RESPONSIBLE_RUN, responsible.getLocalDirectoryPath().toString());
  }

  protected void updateResponsible() {
    AbstractRun candidate = this;
    if (getParent() != null) {
      candidate = getParent();
      if (candidate instanceof RunCapsule) {
        candidate = getParent().getParent();
      }
      while (!candidate.isRunning() || candidate.getState().equals(State.Finalizing) || candidate instanceof RunCapsule) {
        candidate = candidate.getResponsible();
        if (candidate.getParent() == null) {
          break;
        }
      }
    }
    setResponsible(candidate);
  }

  public AbstractRun getResponsible() {
    if (responsible == null) {
      try {
        responsible = getInstance(getWorkspace(), getStringFromProperty(KEY_RESPONSIBLE_RUN));
      } catch (RunNotFoundException e) {
        ErrorLogMessage.issue(e);
      }
    }
    if (responsible == null) {
      ErrorLogMessage.issue(this.getId() + " : not specified responsible run (" + getStringFromProperty(KEY_RESPONSIBLE_RUN) + ")");
    }
    return responsible;
  }

  protected ProcedureRun createHandler(String key) {
    ArchivedConductor conductor = getOwner().getConductor();
    String procedureName = null;
    String procedureKey = null;

    if (key.equals(Conductor.MAIN_PROCEDURE_ALIAS)) {
      procedureName = conductor.getMainProcedureScriptPath().getFileName().toString();
      procedureKey = Conductor.MAIN_PROCEDURE_ALIAS;
    } else {
      List<String> childProcedureNameList = conductor.getChildProcedureNameList();

      if (childProcedureNameList.contains(key)) {
        procedureName = key;
      } else {
        for (String ext : ScriptProcessor.CLASS_NAME_MAP.keySet()) {
          String candidate = key + ext;
          if (childProcedureNameList.contains(candidate)) {
            procedureName = candidate;
            procedureKey = candidate;
            break;
          }
        }
      }
    }

    return ProcedureRun.create(getParent(), procedureName.replaceFirst("\\..*?$", ""), conductor, procedureKey);
  }

  public String getAppealHandler() {
    return getStringFromProperty(KEY_APPEAL_HANDLER, null);
  }

  public void setAppealHandler(String key) {
    ProcedureRun handlerRun = createHandler(key);
    setToProperty(KEY_APPEAL_HANDLER, handlerRun.getLocalDirectoryPath().toString());
  }

  public boolean processAppealHandler(AbstractRun appealer, String message) {
    String handlerName = getAppealHandler();
    if (handlerName != null) {
      ProcedureRun handler = ProcedureRun.getInstance(getWorkspace(), handlerName);
      handler.updateResponsible();
      handler.startHandler(ScriptProcessor.ProcedureMode.APPEALED, this, new ArrayList<>(Arrays.asList(appealer, message)));
      return true;
    }
    return false;
  }

  public boolean appeal(String message) {
    AbstractRun responsible = getResponsible();
    if (responsible != null) {
      return responsible.processAppealHandler(this, message);
    }
    return false;
  }

  public ArrayList<ExecutableRun> getChildrenExecutableRunList() {
    ArrayList<ExecutableRun> childExecutableRunList = new ArrayList<>();

    if (getArrayFromProperty(KEY_CHILDREN_RUN) == null) {
      putNewArrayToProperty(KEY_CHILDREN_RUN);
    }

    for (Object childName  : getArrayFromProperty(KEY_CHILDREN_RUN).toList()) {
      try {
        AbstractRun childRun = AbstractRun.getInstance(getWorkspace(), getLocalDirectoryPath().resolve(childName.toString()).toString());
        if (childRun instanceof ExecutableRun) {
          childExecutableRunList.add((ExecutableRun) childRun);
        }
      } catch (RunNotFoundException e) {
        ErrorLogMessage.issue(e);
      }
    }

    return childExecutableRunList;
  }

  public void registerChildRun(AbstractRun abstractRun) {
    if (getArrayFromProperty(KEY_CHILDREN_RUN) == null) {
      putNewArrayToProperty(KEY_CHILDREN_RUN);
    }
    putToArrayOfProperty(KEY_CHILDREN_RUN, getLocalDirectoryPath().relativize(abstractRun.getLocalDirectoryPath()).toString());
  }

  public void registerChildActiveRun(AbstractRun abstractRun) {
    if (getArrayFromProperty(KEY_ACTIVE_CHILDREN_RUN) == null) {
      putNewArrayToProperty(KEY_ACTIVE_CHILDREN_RUN);
    }
    putToArrayOfProperty(KEY_ACTIVE_CHILDREN_RUN, getLocalDirectoryPath().relativize(abstractRun.getLocalDirectoryPath()).toString());
  }

  private int removeChildActiveRun(AbstractRun abstractRun) {
    if (getArrayFromProperty(KEY_ACTIVE_CHILDREN_RUN) != null) {
      if (abstractRun != null) {
        removeFromArrayOfProperty(KEY_ACTIVE_CHILDREN_RUN, getLocalDirectoryPath().relativize(abstractRun.getLocalDirectoryPath()).toString());
      }
      return getArrayFromProperty(KEY_ACTIVE_CHILDREN_RUN).length();
    }
    return 0;
  }

  public int reportFinishedRun(AbstractRun abstractRun) {
    int remaining = removeChildActiveRun(abstractRun);
    if (remaining <= 0) {
      finish();
    }
    return remaining;
  }

  protected int getChildrenRunSize() {
    JSONArray jsonArray = getArrayFromProperty(KEY_CHILDREN_RUN);
    if (jsonArray != null) {
      return jsonArray.length();
    }
    return 0;
  }

  public ConductorRun getOwner() {
    return owner;
  }

  protected String generateUniqueFileName(String name) {
    name = FileName.removeRestrictedCharacters(name);
    String uniqueName = name;
    int count = 0;
    while (uniqueName.length() <= 0 || Files.exists(getDirectoryPath().resolve(uniqueName))) {
      uniqueName = name + '_' + count++;
    }
    return uniqueName;
  }

  public String getName() {
    return getDirectoryPath().getFileName().toString();
  }

  public String getId() {
    return getLocalDirectoryPath().toString();
  }

  protected boolean started() {
    boolean currentState = isStarted();
    setToProperty(KEY_STARTED, true);
    return currentState;
  }

  public boolean isStarted() {
    return getBooleanFromProperty(KEY_STARTED, false);
  }

  public void setNote(String text) {
    createNewFile(KEY_NOTE_TXT);
    updateFileContents(KEY_NOTE_TXT, text);
  }

  public String getNote() {
    return getFileContents(KEY_NOTE_TXT);
  }

  public void appendErrorNote(String note) {
    createNewFile(KEY_ERROR_NOTE_TXT);
    updateFileContents(KEY_ERROR_NOTE_TXT, getErrorNote().concat(note).concat("\n"));
  }

  public String getErrorNote() {
    return getFileContents(KEY_ERROR_NOTE_TXT);
  }

  public void setParent(AbstractRun parent) {
    this.parent = parent;
    if (parent != null) {
      setToProperty(KEY_PARENT_RUN, parent.getLocalDirectoryPath().toString());
    }
  }

  public ArrayList<String> getFinalizers() {
    if (getArrayFromProperty(KEY_NORMAL_FINALIZER) == null) {
      putNewArrayToProperty(KEY_NORMAL_FINALIZER);
    }
    ArrayList<String> finalizers = new ArrayList<>();
    for (Object o : getArrayFromProperty(KEY_NORMAL_FINALIZER)) {
      finalizers.add(o.toString());
    }
    return finalizers;
  }

  /*
  public void setFinalizers(ArrayList<String> finalizers) {
    if (getArrayFromProperty(KEY_FINALIZER) == null) {
      putNewArrayToProperty(KEY_FINALIZER);
    }
    this.finalizers = new JSONArray(finalizers);
    String finalizersJson = this.finalizers.toString();
    setToProperty(KEY_FINALIZER, finalizersJson);
  }
   */

  public void addFinalizer(String key) {
    if (getArrayFromProperty(KEY_NORMAL_FINALIZER) == null) {
      putNewArrayToProperty(KEY_NORMAL_FINALIZER);
    }

    ArchivedConductor conductor = getOwner().getConductor();
    String procedureName = null;
    String procedureKey = null;
    if (key.equals(Conductor.MAIN_PROCEDURE_ALIAS)) {
      procedureName = conductor.getMainProcedureScriptPath().getFileName().toString();
      procedureKey = Conductor.MAIN_PROCEDURE_ALIAS;
    } else {
      List<String> childProcedureNameList = conductor.getChildProcedureNameList();

      if (childProcedureNameList.contains(key)) {
        procedureName = key;
      } else {
        for (String ext : ScriptProcessor.CLASS_NAME_MAP.keySet()) {
          String candidate = key + ext;
          if (childProcedureNameList.contains(candidate)) {
            procedureName = candidate;
            procedureKey = candidate;
            break;
          }
        }
      }
    }
    //ProcedureRun finalizerRun = ProcedureRun.create(this, procedureName, conductor, procedureKey);
    ProcedureRun finalizerRun = ProcedureRun.create(getParent(), procedureName.replaceFirst("\\..*?$", ""), conductor, procedureKey);
    putToArrayOfProperty(KEY_NORMAL_FINALIZER, finalizerRun.getLocalDirectoryPath().toString());

    /*
    ConductorRun referenceOwner = getOwner();
    if (this instanceof ConductorRun) {
      if (((ConductorRun)this).isActorGroupRun()) {
        referenceOwner = (finalizerReferenceOwner == null ? getOwner() : finalizerReferenceOwner);
      }
    }
    ConductorRun actorRun = ConductorRun.create(getRunNode(),
      (ConductorRun)(this instanceof ExecutableRun || (this instanceof ConductorRun && ((ConductorRun)this).isActorGroupRun())
        ? getParentActor() : this), referenceOwner, key);
    ProcedureRun procedureRun = ProcedureRun.create(this, )
    ArrayList<String> finalizers = getFinalizers();
    finalizers.add(actorRun.getDirectoryPath().toString());
    setFinalizers(finalizers);
     */
  }

  protected void processFinalizers() {
    for (String localPath : getFinalizers()) {
      ProcedureRun finalizer = ProcedureRun.getInstance(getWorkspace(), localPath);
      finalizer.updateResponsible();
      finalizer.startFinalizer(ScriptProcessor.ProcedureMode.START_OR_FINISHED_ALL, this);
    }
  }

  public static Path getBaseDirectoryPath(Workspace workspace) {
    return workspace.getDirectoryPath().resolve(RUN);
  }

  @Override
  public Path getDirectoryPath() {
    return path;
  }

  JSONObject propertyStoreCache = null;
  @Override
  public JSONObject getPropertyStoreCache() {
    return propertyStoreCache;
  }
  @Override
  public void setPropertyStoreCache(JSONObject cache) {
    propertyStoreCache = cache;
  }

  protected void updateVariablesStore(JSONObject variables) {
    //protected void updateParametersStore() {
    if (! Files.exists(getDirectoryPath())) {
      try {
        Files.createDirectories(getDirectoryPath());
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    if (variables == null) {
      variables = new JSONObject();
    }

    Path storePath = getVariablesStorePath();

    try {
      JSONWriter.writeValue(storePath, variables);
      /*
      FileWriter filewriter = new FileWriter(storePath.toFile());
      filewriter.write(variables.toString(2));
      filewriter.close();
       */
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public long getVariablesStoreSize() {
    return getVariablesStorePath().toFile().length();
  }

  private String getFromVariablesStore() {
    Path storePath = getVariablesStorePath();
    String json = "{}";
    if (Files.exists(storePath)) {
      try {
        json = new String(Files.readAllBytes(storePath));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return json;
  }

  public void putVariables(JSONObject valueMap) {
    getVariables(); // init.
    JSONObject map = new JSONObject(getFromVariablesStore());
    if (valueMap != null) {
      for (String key : valueMap.keySet()) {
        map.put(key, valueMap.get(key));
      }
      updateVariablesStore(map);
    }
  }

  public void putVariablesByJson(String json) {
    try {
      putVariables(new JSONObject(json));
    } catch (Exception e) {
      WarnLogMessage.issue(e);
    }
  }

  public void putVariable(String key, Object value) {
    JSONObject obj = new JSONObject();
    obj.put(key, value);
    putVariables(obj);
  }

  public JSONObject getVariables() {
    return new JSONObject(getFromVariablesStore());
  }

  public Object getVariable(String key) {
    return getVariables().get(key);
  }

  private final HashMap<Object, Object> variablesMapWrapper  = new HashMap<Object, Object>() {
    @Override
    public Object get(Object key) {
      return getVariable(key.toString());
    }

    @Override
    public Object put(Object key, Object value) {
      putVariable(key.toString(), value);
      return value;
    }

    @Override
    public String toString() {
      return getVariables().toString();
    }
  };
  public HashMap variables() { return variablesMapWrapper; }
  public HashMap v() { return variablesMapWrapper; }
}
