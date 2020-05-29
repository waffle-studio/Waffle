package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.conductor.TestConductor;
import jp.tkms.waffle.data.util.ResourceFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
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

public class ListenerTemplate extends Data {
  protected static final String TABLE_NAME = "listener_template";
  private static final String KEY_ARGUMENTS = "arguments";
  private static final String KEY_FUNCTIONS = "functions";
  private static final String KEY_LISTENER = "listener";
  private static final String KEY_EXT_RUBY = ".rb";

  private String arguments = null;

  public ListenerTemplate() {
    super();
  }

  public static ArrayList<String> getConductorNameList() {
    return new ArrayList<>(Arrays.asList(
      RubyConductor.class.getCanonicalName(),
      TestConductor.class.getCanonicalName()
    ));
  }

  public ListenerTemplate(UUID id, String name) {
    super(id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ListenerTemplate getInstance(String id) {
    final ListenerTemplate[] conductor = {null};

    handleDatabase(new ListenerTemplate(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new ListenerTemplate(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductor[0];
  }

  public static ListenerTemplate getInstanceByName(String name) {
    final ListenerTemplate[] conductor = {null};

    handleDatabase(new ListenerTemplate(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_NAME + "=?;");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new ListenerTemplate(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductor[0];
  }

  public static ListenerTemplate find(String key) {
    if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(key);
    }
    return getInstanceByName(key);
  }

  public static ArrayList<ListenerTemplate> getList() {
    ArrayList<ListenerTemplate> simulatorList = new ArrayList<>();

    handleDatabase(new ListenerTemplate(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          simulatorList.add(new ListenerTemplate(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name"))
          );
        }
      }
    });

    return simulatorList;
  }

  public static ListenerTemplate create(String name) {
    ListenerTemplate conductor = new ListenerTemplate(UUID.randomUUID(), name);

    if (
      handleDatabase(new ListenerTemplate(), new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into " + TABLE_NAME + "(id,name) values(?,?);");
          statement.setString(1, conductor.getId());
          statement.setString(2, conductor.getName());
          statement.execute();
        }
      })
    ) {
      try {
        Files.createDirectories(conductor.getLocation());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    String fileName = conductor.getScriptFileName();
    Path path = conductor.getLocation().resolve(fileName);
    if (! Files.exists(path)) {
      try {
        FileWriter filewriter = new FileWriter(path.toFile());
        filewriter.write(ResourceFile.getContents("/ruby_listener_template.rb"));
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return conductor;
  }

  public Path getLocation() {
    return Paths.get(Constants.LISTENER_TEMPLATE_DIR.replaceFirst( "\\$\\{NAME\\}", getUnifiedName()));
  }

  public String getScriptFileName() {
    return KEY_LISTENER + "-" + name + KEY_EXT_RUBY;
  }

  public Path getScriptPath() {
    return getLocation().resolve(getScriptFileName());
  }

  public ArrayList<String> getArguments() {
    if (arguments == null) {
      arguments = getStringFromDB(KEY_ARGUMENTS);
    }
    ArrayList<String> list = new ArrayList<>();
    for (Object o : (new JSONArray(arguments)).toList()) {
      list.add(o.toString());
    }
    return list;
  }

  public void setArguments(String json) {
    JSONObject args = new JSONObject(json);
    if (args != null) {
      arguments = args.toString();

      handleDatabase(this, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("update " + getTableName() + " set " + KEY_ARGUMENTS + "=? where " + KEY_ID + "=?;");
          statement.setString(1, arguments);
          statement.setString(2, getId());
          statement.execute();
        }
      });
    }
  }

  public String getFileContents(String fileName) {
    String contents = "";
    try {
      contents = new String(Files.readAllBytes(getLocation().resolve(fileName)));
    } catch (IOException e) {
    }
    return contents;
  }

  public String getMainScriptContents() {
    String mainScript = "";
    try {
      mainScript = new String(Files.readAllBytes(getScriptPath()));
    } catch (IOException e) {
    }
    return mainScript;
  }

  public void updateFileContents(String fileName, String contents) {
    Path path = getLocation().resolve(fileName);
    if (Files.exists(path)) {
      try {
        FileWriter filewriter = new FileWriter(path.toFile());
        filewriter.write(contents);
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void updateMainScriptContents(String contents) {
    if (Files.exists(getScriptPath())) {
      try {
        FileWriter filewriter = new FileWriter(getScriptPath().toFile());
        filewriter.write(contents);
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  protected Updater getDatabaseUpdater() {
    return new Updater() {
      @Override
      String tableName() {
        return TABLE_NAME;
      }

      @Override
      ArrayList<UpdateTask> updateTasks() {
        return new ArrayList<UpdateTask>(Arrays.asList(
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              db.execute("create table " + TABLE_NAME + "("
                + "id,name,"
                + KEY_ARGUMENTS + " default '[]',"
                + KEY_FUNCTIONS + " default '[]',"
                + "timestamp_create timestamp default (DATETIME('now','localtime'))"
                + ");");
            }
          }
        ));
      }
    };
  }
}
