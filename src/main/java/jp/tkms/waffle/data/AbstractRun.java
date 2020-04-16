package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.Sql;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

abstract public class AbstractRun extends ProjectData {
  protected static final String KEY_PARENT = "parent";
  protected static final String KEY_CONDUCTOR = "conductor";
  protected static final String KEY_PARAMETERS = "parameters";
  protected static final String KEY_FINALIZER = "finalizer";

  abstract public boolean isRunning();

  private Conductor conductor = null;
  private ConductorRun parentConductorRun = null;
  private JSONArray finalizers = null;
  private JSONObject parameters = null;

  public AbstractRun(Project project) {
    super(project);
  }

  public AbstractRun(Project project, UUID id, String name) {
    super(project, id, name);
  }

  public ConductorRun getParent() {
    if (parentConductorRun == null) {
      parentConductorRun = ConductorRun.getInstance(getProject(), getFromDB(KEY_PARENT));
    }
    return parentConductorRun;
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

  public void addRawFinalizerScript(String script) {
    ArrayList<String> finalizers = getFinalizers();
    finalizers.add(script);
    setFinalizers(finalizers);
  }

  public void addFinalizer(String name) {
    String fileName = getConductor().getListenerScriptFileName(name);
    addRawFinalizerScript(getConductor().getFileContents(fileName));
  }

  public void putParametersByJson(String json) {
    getParameters(); // init.
    JSONObject valueMap = null;
    try {
      valueMap = new JSONObject(json);
    } catch (Exception e) {
      BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
    JSONObject map = new JSONObject(getFromDB(KEY_PARAMETERS));
    if (valueMap != null) {
      for (String key : valueMap.keySet()) {
        map.put(key, valueMap.get(key));
        parameters.put(key, valueMap.get(key));
      }

      handleDatabase(this, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement = db.preparedStatement("update " + getTableName() + " set " + KEY_PARAMETERS + "=? where " + KEY_ID + "=?;");
          statement.setString(1, map.toString());
          statement.setString(2, getId());
          statement.execute();
        }
      });
    }
  }

  public void putParameter(String key, Object value) {
    JSONObject obj = new JSONObject();
    obj.put(key, value);
    putParametersByJson(obj.toString());
  }

  public JSONObject getParameters() {
    if (parameters == null) {
      parameters = new JSONObject(getFromDB(KEY_PARAMETERS));
    }
    return parameters;
  }

  public Object getParameter(String key) {
    return getParameters().get(key);
  }

  public Object setParameter(String key, Object value) {
    getParameters();
    parameters.put(key, value);
    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Update(db, getTableName(), KEY_PARAMETERS).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, parameters.toString());
        statement.setString(2, getId());
        statement.execute();
      }
    });
    return value;
  }

  private HashMap<Object, Object> parameterWrapper = null;
  public HashMap parameters() {
    if (parameterWrapper == null) {
      parameterWrapper = new HashMap<Object, Object>() {
        @Override
        public Object get(Object key) {
          return getParameter(key.toString());
        }

        @Override
        public Object put(Object key, Object value) {
          //putArgument(key.toString(), value);
          return value;
        }
      };
    }

    return parameterWrapper;
  }
  public HashMap p() { return parameterWrapper; }
  public class ParameterMapInterface extends HashMap<Object, Object> {
    @Override
    public Object get(Object key) {
      return getParameter(key.toString());
    }

    @Override
    public Object put(Object key, Object value) {
      //putArgument(key.toString(), value);
      return value;
    }
  }
}
