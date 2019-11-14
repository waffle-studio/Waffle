package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;

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
  private static final String KEY_POLLING = "polling_interval";
  private static final String KEY_MAX_JOBS = "maximum_jobs";
  private static final String KEY_OS = "os";

  private String workBaseDirectory = null;
  private String xsubDirectory = null;
  private String os = null;
  private Integer pollingInterval = null;
  private Integer maximumNumberOfJobs = null;

  public Host(UUID id, String name) {
    super(id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Host getInstance(String id) {
    final Host[] host = {null};

    handleMainDB(mainUpdater, new Handler() {
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

  public static ArrayList<Host> getList() {
    ArrayList<Host> list = new ArrayList<>();

    handleMainDB(mainUpdater, new Handler() {
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

  public static Host create(String name, String simulationCommand, String versionCommand) {
    return null;
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

  public String getXsubDirectory() {
    if (xsubDirectory == null) {
      xsubDirectory = getFromDB(KEY_XSUB);
    }
    return xsubDirectory;
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

  public Integer getMaximumNumberOfJobs() {
    if (maximumNumberOfJobs == null) {
      maximumNumberOfJobs = Integer.valueOf(getFromDB(KEY_MAX_JOBS));
    }
    return maximumNumberOfJobs;
  }

  @Override
  protected Updater getMainUpdater() {
    return mainUpdater;
  }

  private static Updater mainUpdater = new Updater() {
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
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
              db.execute("insert into " + TABLE_NAME
                + "(id,name," +
                KEY_WORKBASE + "," +
                KEY_XSUB + "," +
                KEY_MAX_JOBS + "," +
                KEY_POLLING +
                ") values('" + LOCAL_UUID.toString() + "','LOCAL','"
                + Environment.LOCAL_WORK_DIR + "','"
                + Environment.LOCAL_XSUB_DIR
                + "',1,5);");

              try {
                Files.createDirectories(Paths.get(Environment.LOCAL_WORK_DIR));
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