package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedConductor;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;
import jp.tkms.waffle.data.project.workspace.conductor.StagedConductor;
import jp.tkms.waffle.data.util.InstanceCache;
import jp.tkms.waffle.data.util.JSONWriter;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.script.ScriptProcessor;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class ConductorRun extends AbstractRun implements DataDirectory {
  public static final String CONDUCTOR_RUN = "CONDUCTOR_RUN";
  public static final String JSON_FILE = CONDUCTOR_RUN + Constants.EXT_JSON;
  public static final String VARIABLES_JSON_FILE = "VARIABLES" + Constants.EXT_JSON;
  public static final String KEY_CONDUCTOR = "conductor";
  protected static final String KEY_CHILDREN_RUN = "children_run";
  protected static final String KEY_ACTIVE_CHILDREN_RUN = "active_children_run";

  private ArchivedConductor conductor;

  private static final InstanceCache<String, ConductorRun> instanceCache = new InstanceCache<>();

  public static String debugReport() {
    return ConductorRun.class.getSimpleName() + ": instanceCacheSize=" + instanceCache.size();
  }

  @Override
  public String getName() {
    return getDirectoryPath().getFileName().toString();
  }

  @Override
  public String getId() {
    return getLocalDirectoryPath().toString();
  }

  public void appendErrorNote(String note) {
    createNewFile(KEY_ERROR_NOTE_TXT);
    updateFileContents(KEY_ERROR_NOTE_TXT, getErrorNote().concat(note).concat("\n"));
  }

  public String getErrorNote() {
    return getFileContents(KEY_ERROR_NOTE_TXT);
  }

  public void start(boolean async) {
    if (async) {
      Main.systemThreadPool.submit(new Runnable() {
        @Override
        public void run() {
          start();
        }
      });
    } else {
      start();
    }
  }

  @Override
  public void start() {
    /*
    started();
    setState(State.Running);
    if (conductor != null) {

      ProcedureRun procedureRun = ProcedureRun.create(this, Conductor.MAIN_PROCEDURE_FILENAME.replaceFirst("\\..*?$", ""), conductor, Conductor.MAIN_PROCEDURE_ALIAS);
      procedureRun.startFinalizer(ScriptProcessor.ProcedureMode.START_OR_FINISHED_ALL, getParent());
    }
    if (getParent() != null) {
      getParent().registerChildRun(this);
    }
    if (!getResponsible().getId().equals(getId())) {
      getResponsible().registerChildActiveRun(this);
    }
    //reportFinishedRun(null);

     */
  }

  @Override
  public void finish() {
    /*
    setState(State.Finalizing);
    processFinalizers();
    if (!getResponsible().getId().equals(getId())) {
      getResponsible().reportFinishedRun(this);
    }
    setState(State.Finished);

     */
  }

  protected Path getVariablesStorePath() {
    return getDirectoryPath().resolve(VARIABLES_JSON_FILE);
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

  public ConductorRun(Workspace workspace, ArchivedConductor conductor, AbstractRun parent, Path path) {
    super(workspace, (ConductorRun) parent, path);
    instanceCache.put(getLocalDirectoryPath().toString(), this);
    setConductor(conductor);
  }

  private void setConductor(ArchivedConductor conductor) {
    this.conductor = conductor;
    if (conductor == null) {
      removeFromProperty(KEY_CONDUCTOR);
    } else {
      setToProperty(KEY_CONDUCTOR, conductor.getArchiveName());
    }
  }

  public ArchivedConductor getConductor() {
    return conductor;
  }

  @Override
  public JSONObject getPropertyStoreCache() {
    return null;
  }

  @Override
  public void setPropertyStoreCache(JSONObject cache) {

  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(JSON_FILE);
  }

  private static ConductorRun create(Workspace workspace, ArchivedConductor conductor, AbstractRun parent, Path path) {
    ConductorRun instance = new ConductorRun(workspace, conductor, parent, path);
    instance.setState(State.Created);
    //instance.updateResponsible();
    if (conductor != null) {
      instance.putVariables(conductor.getDefaultVariables());
    }
    return instance;
  }

  public static ConductorRun create(Workspace workspace, Conductor conductor, String expectedName) {
    String name = expectedName;
    return create(workspace,
      (conductor == null ? null : StagedConductor.getInstance(workspace, conductor).getArchivedInstance()),
      null, workspace.getDirectoryPath().resolve(AbstractRun.RUN).resolve(name));
  }

  public static ConductorRun create(ProcedureRun parent, Conductor conductor, String expectedName) {
    /*
    String name = parent.generateUniqueFileName(expectedName);
    return create(parent.getWorkspace(),
      StagedConductor.getInstance(parent.getWorkspace(), conductor).getArchivedInstance(),
      parent, parent.getDirectoryPath().resolve(name));

     */
    return null;
  }

  public static ConductorRun getInstance(Workspace workspace, String localPathString) {
    ConductorRun instance = instanceCache.get(localPathString);
    if (instance != null) {
      return instance;
    }
    /*

    Path jsonPath = Constants.WORK_DIR.resolve(localPathString).resolve(JSON_FILE);

    if (Files.exists(jsonPath)) {
      try {
        JSONObject jsonObject = new JSONObject(StringFileUtil.read(jsonPath));
        ArchivedConductor conductor = null;
        if (jsonObject.keySet().contains(KEY_CONDUCTOR)) {
          String conductorName = jsonObject.getString(KEY_CONDUCTOR);
          conductor = ArchivedConductor.getInstance(workspace, conductorName);
        }
        AbstractRun parent = null;
        if (jsonObject.keySet().contains(KEY_PARENT_RUN)) {
          String parentPath = jsonObject.getString(KEY_PARENT_RUN);
          parent = AbstractRun.getInstance(workspace, parentPath);
        }
        instance = new ConductorRun(workspace, conductor, parent, jsonPath.getParent());
      } catch (Exception e) {
        ErrorLogMessage.issue(jsonPath.toString() + " : " + ErrorLogMessage.getStackTrace(e));
      }
    }
     */

    return instance;
  }

  static ConductorRun getTestRunConductorRun(ArchivedExecutable executable) {
    return create(executable.getWorkspace(), null, executable.getName());
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

  @Override
  public Path getDirectoryPath() {
    return getPath();
  }
}
