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
  public static final String KEY_RESPONSIBLE_ACTOR = "responsible";
  public static final String KEY_CALLSTACK = "callstack";

  abstract public void start();
  abstract public boolean isRunning();
  abstract public State getState();
  abstract public void setState(State state);

  private ActorGroup actorGroup = null;
  private Actor parentActor = null;
  private Actor responsibleActor = null;
  private JSONArray finalizers = null;
  private JSONArray callstack = null;
  private JSONObject parameters = null;
  protected boolean isStarted = false;
  private RunNode runNode = null;
  private RunNode parentRunNode = null;

  Registry registry;

  public AbstractRun(Project project) {
    super(project);
  }

  public AbstractRun(Project project, UUID id, String name, RunNode runNode) {
    super(project, id, name);
    this.runNode = runNode;

    this.registry = new Registry(getProject());
  }

  public boolean isStarted() {
    return isStarted;
  }

  public Actor getParentActor() {
    if (parentActor == null) {
      parentActor = Actor.getInstance(getProject(), getStringFromDB(KEY_PARENT));
    }
    return parentActor;
  }

  public Actor getResponsibleActor() {
    if (responsibleActor == null) {
      responsibleActor = Actor.getInstance(getProject(), getStringFromDB(KEY_RESPONSIBLE_ACTOR));
    }
    return responsibleActor;
  }

  protected void setResponsibleActor(Actor actor) {
    responsibleActor = actor;
    setToDB(KEY_RESPONSIBLE_ACTOR, actor.getId());
  }

  public Registry getRegistry() {
    return registry;
  }

  @Override
  public void setName(String name) {
    super.setName(getRunNode().rename(name));
  }

  public boolean isRoot() {
    return getParentActor() == null;
  }

  @Override
  public Path getDirectoryPath() {
    return getRunNode().getDirectoryPath();
  }

  public RunNode getRunNode() {
    return runNode;
  }

  protected void setRunNode(RunNode node) {
    runNode = node;
    setToDB(KEY_RUNNODE, node.getId());
  }

  public RunNode getParentRunNode() {
    if (parentRunNode == null) {
      parentRunNode = RunNode.getInstance(getProject(), getStringFromDB(KEY_PARENT_RUNNODE));
    }
    return parentRunNode;
  }

  protected void setParentRunNode(RunNode node) {
    parentRunNode = node;
    setToDB(KEY_PARENT_RUNNODE, node.getId());
  }

  public ActorGroup getActorGroup() {
    ;if (actorGroup == null) {
      actorGroup = ActorGroup.getInstance(getProject(), getStringFromDB(KEY_CONDUCTOR));
    }
    return actorGroup;
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

  public void addFinalizer(String key) {
    Actor actor = Actor.create(getRunNode(), (Actor)(this instanceof SimulatorRun ? getParentActor() : this), getActorGroup(), key);
    ArrayList<String> finalizers = getFinalizers();
    finalizers.add(actor.getId());
    setFinalizers(finalizers);
  }

  public void setCallstack(JSONArray callstack) {
    this.callstack = callstack;
    setToDB(KEY_CALLSTACK, callstack.toString());
  }

  public JSONArray getCallstack() {
    if (callstack == null) {
      callstack = new JSONArray(getStringFromDB(KEY_CALLSTACK));
    }
    return callstack;
  }

  protected static String getCallName(ActorGroup group, String name) {
    return group.getName() + "/" + name;
  }

  public void finish() {
    if (! getState().isRunning()) {
      for (String actorId : getFinalizers()) {
        Actor finalizer = Actor.getInstance(getProject(), actorId);
        finalizer.putVariablesByJson(getVariables().toString());
        finalizer.setResponsibleActor(getResponsibleActor());
        if (finalizer != null) {
          finalizer.start();
        } else {
          WarnLogMessage.issue("the actor(" + actorId + ") is not found");
        }
      }
    }

    if (!isRoot()) {
      getResponsibleActor().update();
    }
  }

  public void appendErrorNote(String note) {
    getRunNode().appendErrorNote(note);
  }

  public String getErrorNote() {
    if (getRunNode() != null) {
      return getRunNode().getErrorNote();
    }
    return "";
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
  };
  public HashMap variables() { return variablesMapWrapper; }
  public HashMap v() { return variablesMapWrapper; }

  public void updateRunNode(RunNode runNode) {
    setToDB(KEY_RUNNODE, runNode.getName());
  }
}
