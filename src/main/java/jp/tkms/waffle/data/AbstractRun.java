package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.JSONWriter;
import jp.tkms.waffle.data.util.State;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

abstract public class AbstractRun extends ProjectData implements DataDirectory, PropertyFile {
  protected static final String KEY_PARENT = "parent";
  protected static final String KEY_ACTOR_GROUP = "actor_group";
  protected static final String KEY_FINALIZER = "finalizer";
  protected static final String KEY_VARIABLES = "variables";
  protected static final String KEY_STATE = "state";
  protected static final String KEY_TIMESTAMP_CREATE = "timestamp_create";
  public static final String KEY_RUNNODE = "runnode";
  public static final String KEY_PARENT_RUNNODE = "parent_runnode";
  public static final String KEY_RESPONSIBLE_ACTOR = "responsible";
  public static final String KEY_CALLSTACK = "callstack";
  public static final String KEY_OWNER = "owner";

  abstract public boolean isRunning();
  abstract public State getState();
  abstract public void setState(State state);

  protected UUID id;
  private Path path;
  private String name = null;
  private ActorGroup actorGroup = null;
  private ActorRun parentActorRun = null;
  private ActorRun responsibleActorRun = null;
  private JSONArray finalizers = null;
  private JSONArray callstack = null;
  //private JSONObject variables = null;
  protected boolean isStarted = false;
  private RunNode runNode = null;
  private RunNode parentRunNode = null;
  private ActorRun finalizerReferenceOwner = null;
  private ActorRun owner = null;

  Registry registry;

  public AbstractRun(Project project, UUID id, Path path) {
    super(project);
    this.path = path;
    this.id = id;

    this.registry = new Registry(getProject());
  }

  public String getId() {
    return id.toString();
  }

  public UUID getUuid() {
    return id;
  }

  @Override
  public Path getDirectoryPath() {
    if (runNode != null) {
      return path = runNode.getDirectoryPath();
    }
    return path;
  }

  public String getName() {
    if (name == null) {
      name = getRunNode().getSimpleName();
    }
    return name;
  }

  public void start() {
    if (!isRoot()) {
      getResponsibleActor().registerActiveRun(this);
    }
  }

  public boolean isStarted() {
    return isStarted;
  }

  public ActorRun getParentActor() {
    if (parentActorRun == null) {
      parentActorRun = ActorRun.getInstance(getProject(), getStringFromProperty(KEY_PARENT));
    }
    return parentActorRun;
  }

  public ActorRun getResponsibleActor() {
    if (responsibleActorRun == null) {
      responsibleActorRun = ActorRun.getInstance(getProject(), getStringFromProperty(KEY_RESPONSIBLE_ACTOR));
    }
    return responsibleActorRun;
  }

  protected void setResponsibleActor(ActorRun actorRun) {
    responsibleActorRun = actorRun;
    setToProperty(KEY_RESPONSIBLE_ACTOR, actorRun.getId());
  }

  public Registry getRegistry() {
    return registry;
  }

  public void setName(String name) {
    this.name = getRunNode().rename(name);
  }

  public boolean isRoot() {
    return ActorRun.ROOT_UUID.equals(getUuid());
  }

  public RunNode getRunNode() {
    if (runNode == null) {
      runNode = RunNode.getInstance(getProject(), getStringFromProperty(KEY_RUNNODE));
    }
    return runNode;
  }

  public ArrayList<SimulatorRun> getChildSimulationRunList() {
    ArrayList<SimulatorRun> childSimulationRunList = new ArrayList<>();

    for (RunNode node : getRunNode().getList()) {
      if (node instanceof SimulatorRunNode) {
        try {
          childSimulationRunList.add(SimulatorRun.getInstance(node.getProject(), node.getId()));
        } catch (RunNotFoundException e) {
          e.printStackTrace();
        }
      }
    }

    return childSimulationRunList;
  }

  public ActorRun getOwner() {
    if (owner == null) {
      owner = ActorRun.getInstance(getProject(), getStringFromProperty(KEY_OWNER));
    }
    return owner;
  }

  protected void setRunNode(RunNode node) {
    runNode = node;
    setToProperty(KEY_RUNNODE, node.getId());
    path = runNode.getDirectoryPath();
  }

  public RunNode getParentRunNode() {
    if (parentRunNode == null) {
      parentRunNode = RunNode.getInstance(getProject(), getStringFromProperty(KEY_PARENT_RUNNODE));
    }
    return parentRunNode;
  }

  protected void setParentRunNode(RunNode node) {
    parentRunNode = node;
    setToProperty(KEY_PARENT_RUNNODE, node.getId());
  }

  public ActorGroup getActorGroup() {
    ;if (actorGroup == null) {
      actorGroup = ActorGroup.getInstance(getProject(), getStringFromProperty(KEY_ACTOR_GROUP));
    }
    return actorGroup;
  }

  public ArrayList<String> getFinalizers() {
    if (finalizers == null) {
      finalizers = new JSONArray(getStringFromProperty(KEY_FINALIZER, "[]"));
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
    setToProperty(KEY_FINALIZER, finalizersJson);
  }

  public void addFinalizer(String key) {
    ActorRun referenceOwner = getOwner();
    if (this instanceof ActorRun) {
      if (((ActorRun)this).isActorGroupRun()) {
        referenceOwner = (finalizerReferenceOwner == null ? getOwner() : finalizerReferenceOwner);
      }
    }
    ActorRun actorRun = ActorRun.create(getRunNode(),
      (ActorRun)(this instanceof SimulatorRun || (this instanceof ActorRun && ((ActorRun)this).isActorGroupRun())
        ? getParentActor() : this), referenceOwner, key);
    ArrayList<String> finalizers = getFinalizers();
    finalizers.add(actorRun.getId());
    setFinalizers(finalizers);
  }

  protected void setFinalizerReference(ActorRun actorRun) {
    this.finalizerReferenceOwner = actorRun;
  }

  public void setCallstack(JSONArray callstack) {
    this.callstack = callstack;
    setToProperty(KEY_CALLSTACK, callstack.toString());
  }

  public JSONArray getCallstack() {
    if (callstack == null) {
      callstack = new JSONArray(getStringFromProperty(KEY_CALLSTACK, "[]"));
    }
    return new JSONArray(callstack.toString());
  }

  protected static String getCallName(ActorGroup group, String name) {
    if (group == null) {
      return "?/?";
    }
    return group.getName() + "/" + name;
  }

  public void finish() {
    Main.systemThreadPool.submit(() -> {
    /*
      run finalizers
     */
      if (getState().equals(State.Finished)) {
        for (String actorId : getFinalizers()) {
          ActorRun finalizer = ActorRun.getInstance(getProject(), actorId);
          //finalizer.putVariablesByJson(getVariables().toString());
          finalizer.setResponsibleActor(getResponsibleActor());
          /*
          ActorRun responsibleActor = getResponsibleActor();
          if (this instanceof SimulatorRun || (this instanceof ActorRun && ((ActorRun)this).isActorGroupRun())) {
            if (getResponsibleActor().getRunNode() instanceof InclusiveRunNode && getResponsibleActor().getParentActor() != null) {
              finalizer.setResponsibleActor(getResponsibleActor().getParentActor());
            }
          }
          finalizer.setResponsibleActor(responsibleActor);
           */
          if (finalizer != null) {
            finalizer.start(this);
          } else {
            WarnLogMessage.issue("the actor(" + actorId + ") is not found");
          }
        }
      }

    /*
      send a message finished to a responsible actor.
     */
      if (!isRoot()) {
        getResponsibleActor().postMessage(this, getState().name());
      }
    });
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

  /*
  public void putVariablesByJson(String json) {
    getVariables(); // init.
    JSONObject valueMap = null;
    try {
      valueMap = new JSONObject(json);
    } catch (Exception e) {
      WarnLogMessage.issue(e);
    }
    if (valueMap != null) {
      if (!getOwner().getId().equals(getId())) {
        getOwner().putVariablesByJson(json);
      }

      for (String key : valueMap.keySet()) {
        variables.put(key, valueMap.get(key));
      }

      setToProperty(KEY_VARIABLES, variables.toString());
    }
  }

  public void putVariable(String key, Object value) {
    JSONObject obj = new JSONObject();
    obj.put(key, value);
    putVariablesByJson(obj.toString());
  }

  public JSONObject getVariables() {
    if (getOwner().getId().equals(getId())) {
      if (variables == null) {
        variables = new JSONObject(getStringFromProperty(KEY_VARIABLES, "{}"));
      }
    } else {
      variables = getOwner().getVariables();
    }
    return variables;
  }

  public Object getVariable(String key) {
    return getVariables().get(key);
  }
  */

  protected void updateVariablesStore(JSONObject variables) {
    //protected void updateParametersStore() {
    if (! Files.exists(getDirectoryPath())) {
      try {
        Files.createDirectories(getDirectoryPath());
      } catch (IOException e) {
        e.printStackTrace();
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

  private Path getVariablesStorePath() {
    Path storePath = getDirectoryPath().resolve(KEY_VARIABLES + Constants.EXT_JSON);
    if (this instanceof ActorRun) {
      storePath = ((ActorRun)this).getDataDirectory(getUuid()).resolve(KEY_VARIABLES + Constants.EXT_JSON);
    }
    return storePath;
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

  public void putVariablesByJson(String json) {
    getVariables(); // init.
    JSONObject valueMap = null;
    try {
      valueMap = new JSONObject(json);
    } catch (Exception e) {
      BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      e.printStackTrace();
    }
    //JSONObject map = new JSONObject(getFromDB(KEY_PARAMETERS));
    JSONObject map = new JSONObject(getFromVariablesStore());
    if (valueMap != null) {
      if (!getOwner().getId().equals(getId())) {
        getOwner().putVariablesByJson(json);
      }

      for (String key : valueMap.keySet()) {
        map.put(key, valueMap.get(key));
        //parameters.put(key, valueMap.get(key));
      }

      updateVariablesStore(map);
    }
  }

  public void putVariable(String key, Object value) {
    JSONObject obj = new JSONObject();
    obj.put(key, value);
    putVariablesByJson(obj.toString());
  }

  public JSONObject getVariables() {
    if (getOwner().getId().equals(getId())) {
      //if (variables == null) {
        return new JSONObject(getFromVariablesStore());
      //}
    }
    return getOwner().getVariables();
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
    setToProperty(KEY_RUNNODE, runNode.getName());
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
}
