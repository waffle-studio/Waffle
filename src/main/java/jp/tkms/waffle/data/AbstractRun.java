package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.State;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

abstract public class AbstractRun implements EntityDatabase {
  protected static final String KEY_NAME = "name";
  protected static final String KEY_ACTOR = "actor";
  protected static final String KEY_PARENT = "parent";
  protected static final String KEY_ACTOR_GROUP = "actor_group";
  protected static final String KEY_FINALIZER = "finalizer";
  protected static final String KEY_VARIABLES = "variables";
  protected static final String KEY_STATE = "state";
  protected static final String KEY_TIMESTAMP_CREATE = "timestamp_create";
  public static final String KEY_RUNNODE = "runnode";
  public static final String KEY_PARENT_RUNNODE = "parent_runnode";
  public static final String KEY_RESPONSIBLE_ACTOR = "responsible";

  abstract public void start();
  abstract public boolean isRunning();
  abstract public State getState();
  abstract public void setState(State state);

  private ActorGroup actorGroup = null;
  private Actor parentActor = null;
  private Actor responsibleActor = null;
  private JSONArray finalizers = null;
  private JSONObject parameters = null;
  protected boolean isStarted = false;
  private RunNode runNode = null;

  UUID id;
  String name;
  Workspace workspace;
  Registry registry;

  public AbstractRun(Workspace workspace, UUID id, String name) {
    this.workspace = workspace;
    this.id = id;
    this.name = name;

    this.registry = new Registry(workspace.getProject());
  }

  public String getId() {
    return id.toString();
  }

  public String getShortId() {
    return getShortId(getUuid());
  }

  public static String getShortId(UUID id) {
    return id.toString().replaceFirst("-.*$", "");
  }

  public UUID getUuid() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
    setToDB(KEY_NAME, name);
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public Project getProject() {
    return getWorkspace().getProject();
  }

  @Override
  public Database getDatabase() {
    return getWorkspace().getDatabase();
  }

  @Override
  public String getEntityId() {
    return getId();
  }

  @Override
  public String getEntityTable() {
    return KEY_ACTOR;
  }

  public boolean isStarted() {
    return isStarted;
  }

  public Actor getParentActor() {
    if (parentActor == null) {
      parentActor = Actor.getInstance(getWorkspace(), getStringFromDB(KEY_PARENT));
    }
    return parentActor;
  }

  public Actor getResponsibleActor() {
    if (responsibleActor == null) {
      responsibleActor = Actor.getInstance(getWorkspace(), getStringFromDB(KEY_RESPONSIBLE_ACTOR));
    }
    return responsibleActor;
  }

  protected void setResponsibleActor(Actor actor) {
    setToDB(KEY_RESPONSIBLE_ACTOR, actor.getId());
    responsibleActor = actor;
  }

  public Registry getRegistry() {
    return registry;
  }

  public RunNode getRunNode() {
    if (runNode == null) {
      runNode = RunNode.getInstance(getWorkspace(), Paths.get(getStringFromDB(KEY_RUNNODE)));
    }
    return runNode;
  }

  protected void setRunNode(RunNode node) {
    runNode = node;
    setToDB(KEY_RUNNODE, node.getPath().toString());
  }

  public boolean isRoot() {
    return getParentActor() == null;
  }

  public ActorGroup getActorGroup() {
    if (actorGroup == null) {
      actorGroup = ActorGroup.getInstanceByName(getWorkspace().getProject(), getStringFromDB(KEY_ACTOR_GROUP));
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

  public void finish() {
    if (! getState().isRunning()) {
      for (String actorId : getFinalizers()) {
        Actor finalizer = Actor.getInstance(getWorkspace(), actorId);
        finalizer.setResponsibleActor(getResponsibleActor());
        if (finalizer != null) {
          finalizer.start(this);
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
    return getRunNode().getErrorNote();
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

      setToDB(KEY_VARIABLES, parameters.toString());
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
    setToDB(KEY_RUNNODE, getRunNode().getPath().toString());
  }
}
