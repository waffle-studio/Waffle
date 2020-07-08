package jp.tkms.waffle.data;

import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.State;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

abstract public class AbstractRun extends ProjectData implements DataDirectory {
  protected static final String KEY_PARENT = "parent";
  protected static final String KEY_CONDUCTOR = "conductor";
  protected static final String KEY_FINALIZER = "finalizer";
  protected static final String KEY_VARIABLES = "variables";
  protected static final String KEY_STATE = "state";
  protected static final String KEY_TIMESTAMP_CREATE = "timestamp_create";
  public static final String KEY_RUNNODE = "runnode";
  public static final String KEY_PARENT_RUNNODE = "parent_runnode";

  abstract public void start();
  abstract public boolean isRunning();
  abstract public State getState();
  abstract public void setState(State state);

  private ActorGroup conductor = null;
  private Actor parentConductorRun = null;
  private JSONArray finalizers = null;
  private JSONObject parameters = null;
  protected boolean isStarted = false;
  private RunNode runNode = null;
  private RunNode parentRunNode = null;

  Registry registry;

  public AbstractRun(Project project) {
    super(project);
  }

  public AbstractRun(Project project, UUID id, String name, RunNode runNode) {
    super(project, id, runNode.getSimpleName());
    this.runNode = runNode;

    this.registry = new Registry(getProject());
  }

  public boolean isStarted() {
    return isStarted;
  }

  public Actor getParent() {
    if (parentConductorRun == null) {
      parentConductorRun = Actor.getInstance(getProject(), getStringFromDB(KEY_PARENT));
    }
    return parentConductorRun;
  }

  public Registry getRegistry() {
    return registry;
  }

  @Override
  public void setName(String name) {
    super.setName(runNode.rename(name));
  }

  public boolean isRoot() {
    return getParent() == null;
  }

  @Override
  public Path getDirectoryPath() {
    return runNode.getDirectoryPath();
  }

  public RunNode getRunNode() {
    return runNode;
  }

  protected void setRunNode(RunNode node) {
    setToDB(KEY_RUNNODE, node.getId());
    runNode = node;
  }

  public RunNode getParentRunNode() {
    if (parentRunNode == null) {
      parentRunNode = RunNode.getInstance(getProject(), getStringFromDB(KEY_PARENT_RUNNODE));
    }
    return parentRunNode;
  }

  protected void setParentRunNode(RunNode node) {
    setToDB(KEY_PARENT_RUNNODE, node.getId());
    runNode = node;
  }

  public ActorGroup getConductor() {
    if (conductor == null) {
      conductor = ActorGroup.getInstance(getProject(), getStringFromDB(KEY_CONDUCTOR));
    }
    return conductor;
  }

  public ArrayList<String> getFinalizers() {
    if (finalizers == null) {
      finalizers = new JSONArray(getStringFromDB(KEY_FINALIZER));
    }
    ArrayList<String> stringList = new ArrayList<>();
    for (Object o : finalizers.toList()) {
      stringList.add(o.toString());
    }
    return stringList;
  }

  public void setFinalizers(ArrayList<String> finalizers) {
    this.finalizers = new JSONArray(finalizers);
    String finalizersJson = this.finalizers.toString();
    setToDB(KEY_FINALIZER, finalizersJson);
  }

  public void addFinalizer(String id) {
    ArrayList<String> finalizers = getFinalizers();
    finalizers.add(id);
    setFinalizers(finalizers);
  }

  public void appendErrorNote(String note) {
    getRunNode().appendErrorNote(note);
  }

  public String getErrorNote() {
    return getRunNode().getErrorNote();
  }

  protected void updateVariablesStore() {
    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Update(db, getTableName(),
          Sql.Value.equal( KEY_VARIABLES, getVariables().toString() )
        ).where(Sql.Value.equal(KEY_ID, getId())).execute();
      }
    });
  }

  public void putVariablesByJson(String json) {
    getVariables(); // init.
    JSONObject valueMap = null;
    try {
      valueMap = new JSONObject(json);
    } catch (Exception e) {
      WarnLogMessage.issue(e);
    }
    JSONObject map = new JSONObject(getStringFromDB(KEY_VARIABLES));
    if (valueMap != null) {
      for (String key : valueMap.keySet()) {
        map.put(key, valueMap.get(key));
        parameters.put(key, valueMap.get(key));
      }

      updateVariablesStore();
    }
  }

  public void putVariable(String key, Object value) {
    JSONObject obj = new JSONObject();
    obj.put(key, value);
    putVariablesByJson(obj.toString());
  }

  public JSONObject getVariables() {
    if (parameters == null) {
      parameters = new JSONObject(getStringFromDB(KEY_VARIABLES));
    }
    return parameters;
  }

  public Object getVariable(String key) {
    return getVariables().get(key);
  }


  private HashMap<Object, Object> variablesWrapper = null;
  public HashMap variables() {
    if (variablesWrapper == null) {
      variablesWrapper = new HashMap<Object, Object>() {
        @Override
        public Object get(Object key) {
          return getVariable(key.toString());
        }

        @Override
        public Object put(Object key, Object value) {
          putVariable(key.toString(), value);
          return value;
        }
      };
    }

    return variablesWrapper;
  }
  public HashMap v() { return variables(); }

  public void updateRunNode(RunNode runNode) {
    setToDB(KEY_RUNNODE, runNode.getName());
  }
}
