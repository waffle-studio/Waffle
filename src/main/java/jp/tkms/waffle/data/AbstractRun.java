package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.Sql;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

abstract public class AbstractRun extends ProjectData {
  protected static final String KEY_TRIAL = "trial";
  protected static final String KEY_PARENT = "parent";
  protected static final String KEY_CONDUCTOR = "conductor";
  protected static final String KEY_FINALIZER = "finalizer";
  protected static final String KEY_VARIABLES = "variables";
  protected static final String KEY_STATE = "state";
  protected static final String KEY_ERROR_NOTE = "error_note";
  protected static final String KEY_TIMESTAMP_CREATE = "timestamp_create";

  abstract public void start();
  abstract public boolean isRunning();

  private Conductor conductor = null;
  private ConductorRun parentConductorRun = null;
  private JSONArray finalizers = null;
  private JSONObject parameters = null;
  protected boolean isStarted = false;

  public AbstractRun(Project project) {
    super(project);
  }

  public AbstractRun(Project project, UUID id, String name) {
    super(project, id, name);
  }

  public boolean isStarted() {
    return isStarted;
  }

  public ConductorRun getParent() {
    if (parentConductorRun == null) {
      parentConductorRun = ConductorRun.getInstance(getProject(), getFromDB(KEY_PARENT));
    }
    return parentConductorRun;
  }

  public boolean isRoot() {
    return getParent() == null;
  }

  public Path getPath() {
    if (isRoot()) {
      return getProject().getLocation().resolve(KEY_TRIAL);
    }
    return getParent().getPath().resolve(getId());
  }


  public Conductor getConductor() {
    if (conductor == null) {
      conductor = Conductor.getInstance(getProject(), getFromDB(KEY_CONDUCTOR));
    }
    return conductor;
  }

  public ArrayList<String> getFinalizers() {
    if (finalizers == null) {
      finalizers = new JSONArray(getFromDB(KEY_FINALIZER));
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

    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("update " + getTableName() + " set " + KEY_FINALIZER + "=? where " + KEY_ID + "=?;");
        statement.setString(1, finalizersJson);
        statement.setString(2, getId());
        statement.execute();
      }
    });
  }

  public void appendErrorNote(String note) {
    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("update " + getTableName() + " set " + KEY_ERROR_NOTE + "=" + KEY_ERROR_NOTE + "||? where " + KEY_ID + "=?;");
        statement.setString(1, note + '\n');
        statement.setString(2, getId());
        statement.execute();
      }
    });
  }

  public String getErrorNote() {
    return getFromDB(KEY_ERROR_NOTE);
  }

  public void addRawFinalizerScript(String script) {
    ArrayList<String> finalizers = getFinalizers();
    finalizers.add(script);
    setFinalizers(finalizers);
  }

  public void addFinalizer(String name) {
    String fileName = getConductor().getListenerScriptFileName(name);
    addRawFinalizerScript(getConductor().getFileContents(fileName));
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
      BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      e.printStackTrace();
    }
    JSONObject map = new JSONObject(getFromDB(KEY_VARIABLES));
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
      parameters = new JSONObject(getFromDB(KEY_VARIABLES));
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
          //putArgument(key.toString(), value);
          return value;
        }
      };
    }

    return variablesWrapper;
  }
  public HashMap v() { return variables(); }
}
