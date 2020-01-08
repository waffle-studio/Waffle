package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
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
import java.util.Map;
import java.util.UUID;

public class Host extends Data {
  private static final String TABLE_NAME = "host";
  private static final UUID LOCAL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private static final String KEY_WORKBASE = "work_base_dir";
  private static final String KEY_XSUB = "xsub_dir";
  private static final String KEY_POLLING = "polling_interval";
  private static final String KEY_MAX_JOBS = "maximum_jobs";
  private static final String KEY_OS = "os";
  private static final String KEY_PARAMETERS = "parameters";

  private String hostName = null;
  private String workBaseDirectory = null;
  private String xsubDirectory = null;
  private String os = null;
  private Integer pollingInterval = null;
  private Integer maximumNumberOfJobs = null;
  private JSONObject parameters = null;

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
        PreparedStatement statement = new Sql.Insert(db, TABLE_NAME,
          KEY_ID, KEY_NAME, KEY_WORKBASE, KEY_XSUB, KEY_MAX_JOBS, KEY_POLLING).toPreparedStatement();
        statement.setString(1, host.getId());
        statement.setString(2, host.getName());
        statement.setString(3, "/tmp/waffle");
        statement.setString(4, "");
        statement.setInt(5, 1);
        statement.setInt(6, 10);
        statement.execute();
      }
    });
    return host;
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
    for (String key : AbstractSubmitter.getXsubParameter(this).keySet()) {
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
      parameters = AbstractSubmitter.getParametersWithXsubParameter(this);
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
    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Update(db, getTableName(), KEY_PARAMETERS).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, jsonObject.toString());
        statement.setString(2, getId());
        statement.execute();
      }
    });
  }

  public Object setParameter(String key, Object value) {
    getParameters();
    parameters.put(key, value);
    setParameters(parameters);
    return value;
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
                KEY_PARAMETERS + " default '{}'," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");

              PreparedStatement statement = new Sql.Insert(db, TABLE_NAME,
                KEY_ID, KEY_NAME,
                KEY_WORKBASE,
                KEY_XSUB,
                KEY_MAX_JOBS,
                KEY_POLLING
                ).toPreparedStatement();
              statement.setString(1, LOCAL_UUID.toString());
              statement.setString(2, "LOCAL");
              statement.setString(3, Constants.LOCAL_WORK_DIR);
              statement.setString(4, Constants.LOCAL_XSUB_DIR);
              statement.setInt(5, 1);
              statement.setInt(6, 5);
              statement.execute();

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
          }
        ));
      }
    };
  }
}
