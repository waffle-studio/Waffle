package jp.tkms.waffle.data;

import com.jcraft.jsch.JSchException;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.util.HostState;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Host extends Data {
  private static final String TABLE_NAME = "host";
  private static final UUID LOCAL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private static final String KEY_WORKBASE = "work_base_dir";
  private static final String KEY_XSUB = "xsub_dir";
  private static final String KEY_XSUB_TEMPLATE = "xsub_template";
  private static final String KEY_POLLING = "polling_interval";
  private static final String KEY_MAX_JOBS = "maximum_jobs";
  private static final String KEY_OS = "os";
  private static final String KEY_PARAMETERS = "parameters";
  private static final String KEY_STATE = "state";

  private String hostName = null;
  private String workBaseDirectory = null;
  private String xsubDirectory = null;
  private String os = null;
  private Integer pollingInterval = null;
  private Integer maximumNumberOfJobs = null;
  private JSONObject parameters = null;
  private JSONObject xsubTemplate = null;

  public Host(UUID id, String name) {
    super(id, name);
  }

  public Host() { }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Host getInstance(String id) {
    final Host[] host = {null};

    handleDatabase(new Host(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          host[0] = new Host(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return host[0];
  }

  public static Host getInstanceByName(String name) {
    final Host[] host = {null};

    handleDatabase(new Host(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where name=?;");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          host[0] = new Host(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return host[0];
  }

  public static Host find(String key) {
    if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(key);
    }
    return getInstanceByName(key);
  }

  public static ArrayList<Host> getList() {
    ArrayList<Host> list = new ArrayList<>();

    handleDatabase(new Host(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          list.add(new Host(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name"))
          );
        }
      }
    });

    return list;
  }

  public static Host create(String name) {
    Host host = new Host(UUID.randomUUID(), name);
    handleDatabase(new Host(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Insert(db, TABLE_NAME,
          Sql.Value.equal( KEY_ID, host.getId() ),
          Sql.Value.equal( KEY_NAME, host.getName() ),
          Sql.Value.equal( KEY_WORKBASE, "/tmp/waffle" ),
          Sql.Value.equal( KEY_XSUB, "" ),
          Sql.Value.equal( KEY_MAX_JOBS, 1 ),
          Sql.Value.equal( KEY_POLLING, 10 )
          ).execute();
      }
    });
    return host;
  }

  public void update() {
    try {
      JSONObject jsonObject = AbstractSubmitter.getXsubTemplate(this, false);
      setXsubTemplate(jsonObject);
      setState(HostState.Viable);
    } catch (RuntimeException e) {
      setState(HostState.Unviable);
    }
  }

  private void setState(HostState state) {
    setIntToDB(KEY_STATE, state.ordinal());
  }

  public HostState getState() {
    return HostState.valueOf(getIntFromDB(KEY_STATE));
  }

  public Path getLocation() {
    Path path = Paths.get( TABLE_NAME + File.separator + name + '_' + shortId );
    return path;
  }

  public boolean isLocal() {
    return LOCAL_UUID.equals(id);
  }

  public String getWorkBaseDirectory() {
    if (workBaseDirectory == null) {
      workBaseDirectory = getFromDB(KEY_WORKBASE);
    }
    return workBaseDirectory;
  }

  public void setWorkBaseDirectory(String workBaseDirectory) {
    if (
      setStringToDB(KEY_WORKBASE, workBaseDirectory)
    ) {
      this.workBaseDirectory = workBaseDirectory;
    }
  }

  public String getXsubDirectory() {
    if (xsubDirectory == null) {
      xsubDirectory = getFromDB(KEY_XSUB);
    }
    return xsubDirectory;
  }

  public void setXsubDirectory(String xsubDirectory) {
    if (
      setStringToDB(KEY_XSUB, xsubDirectory)
    ) {
      this.xsubDirectory = xsubDirectory;
    }
  }

  public String getOs() {
    if (os == null) {
      os = getFromDB(KEY_OS);
    }
    return os;
  }

  public String getDirectorySeparetor() {
    String directorySeparetor = "/";
    if (getOs().equals("U")) {
      directorySeparetor = "/";
    }
    return directorySeparetor;
  }

  public Integer getPollingInterval() {
    if (pollingInterval == null) {
      pollingInterval = Integer.valueOf(getFromDB(KEY_POLLING));
    }
    return pollingInterval;
  }

  public void setPollingInterval(Integer pollingInterval) {
    if (
      setIntToDB(KEY_POLLING, pollingInterval)
    ) {
      this.pollingInterval = pollingInterval;
    }
  }

  public Integer getMaximumNumberOfJobs() {
    if (maximumNumberOfJobs == null) {
      maximumNumberOfJobs = Integer.valueOf(getFromDB(KEY_MAX_JOBS));
    }
    return maximumNumberOfJobs;
  }

  public void setMaximumNumberOfJobs(Integer maximumNumberOfJobs) {
    if (
      setIntToDB(KEY_MAX_JOBS, maximumNumberOfJobs)
    ) {
      this.maximumNumberOfJobs = maximumNumberOfJobs;
    }
  }

  public JSONObject getXsubParameters() {
    JSONObject jsonObject = new JSONObject();
    for (String key : getXsubParametersTemplate().keySet()) {
      jsonObject.put(key, getParameter(key));
    }
    return jsonObject;
  }

  public JSONObject getParametersWithoutXsubParameter() {
    JSONObject parameters = AbstractSubmitter.getParameters(this);
    JSONObject jsonObject = new JSONObject(getFromDB(KEY_PARAMETERS));
    for (String key : jsonObject.keySet()) {
      parameters.put(key, jsonObject.get(key));
    }
    return parameters;
  }

  public JSONObject getParameters() {
    if (parameters == null) {
      parameters = getDefaultParametersWithXsubParametersTemplate();
      JSONObject jsonObject = new JSONObject(getFromDB(KEY_PARAMETERS));
      for (String key : jsonObject.keySet()) {
        parameters.put(key, jsonObject.get(key));
      }
    }
    return parameters;
  }

  public Object getParameter(String key) {
    return getParameters().get(key);
  }

  public void setParameters(JSONObject jsonObject) {
    if (
      setStringToDB(KEY_PARAMETERS, jsonObject.toString())
    ) {
      this.parameters = jsonObject;
    }
  }

  public Object setParameter(String key, Object value) {
    getParameters();
    parameters.put(key, value);
    setParameters(parameters);
    return value;
  }

  public JSONObject getXsubTemplate() {
    if (xsubTemplate == null) {
      xsubTemplate = new JSONObject(getFromDB(KEY_XSUB_TEMPLATE));
    }
    return xsubTemplate;
  }

  public void setXsubTemplate(JSONObject jsonObject) {
    if (
      setStringToDB(KEY_XSUB_TEMPLATE, jsonObject.toString())
    ) {
      this.xsubTemplate = jsonObject;
    }
  }

  public JSONObject getDefaultParametersWithXsubParametersTemplate() {
    JSONObject jsonObject = AbstractSubmitter.getInstance(this).defaultParameters(this);

    try {
      JSONObject object = getXsubTemplate().getJSONObject("parameters");
      for (String key : object.toMap().keySet()) {
        jsonObject.put(key, object.getJSONObject(key).get("default"));
      }
    } catch (Exception e) {}

    return jsonObject;
  }

  public JSONObject getXsubParametersTemplate() {
    JSONObject jsonObject = new JSONObject();

    try {
      JSONObject object = getXsubTemplate().getJSONObject("parameters");
      for (String key : object.toMap().keySet()) {
        jsonObject.put(key, object.getJSONObject(key).get("default"));
      }
    } catch (Exception e) {}

    return jsonObject;
  }

  @Override
  protected Updater getDatabaseUpdater() {
    return new Updater() {
      @Override
      String tableName() {
        return TABLE_NAME;
      }

      @Override
      ArrayList<Updater.UpdateTask> updateTasks() {
        return new ArrayList<Updater.UpdateTask>(Arrays.asList(
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              db.execute("create table " + TABLE_NAME + "(id,name," +
                KEY_WORKBASE + "," +
                KEY_XSUB + "," +
                KEY_MAX_JOBS + "," +
                KEY_POLLING + "," +
                KEY_XSUB_TEMPLATE + " default '{}'," +
                KEY_PARAMETERS + " default '{}'," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");

              new Sql.Insert(db, TABLE_NAME,
                Sql.Value.equal( KEY_ID, LOCAL_UUID.toString() ),
                Sql.Value.equal( KEY_NAME, "LOCAL" ),
                Sql.Value.equal( KEY_WORKBASE, Constants.LOCAL_WORK_DIR ),
                Sql.Value.equal( KEY_XSUB, Constants.LOCAL_XSUB_DIR ),
                Sql.Value.equal( KEY_MAX_JOBS, 1 ),
                Sql.Value.equal( KEY_POLLING, 5 )
              ).execute();

              try {
                Files.createDirectories(Paths.get(Constants.LOCAL_WORK_DIR));
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          },
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              db.execute("alter table " + TABLE_NAME + " add " + KEY_OS + " default 'U';");
            }
          },
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              db.execute("alter table " + TABLE_NAME + " add " + KEY_STATE + " default " + HostState.Unviable.ordinal() + ";");
            }
          }
        ));
      }
    };
  }
}
