package jp.tkms.waffle.data;

import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.conductor.TestConductor;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.ValueType;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class ConductorArgument extends ProjectData {
  protected static final String TABLE_NAME = "conductor_argument";
  private static final String KEY_CONDUCTOR = "conductor";
  private static final String KEY_TYPE = "type";
  private static final String KEY_DEFAULT_VALUE = "default_value";

  private Conductor conductor;
  private ValueType valueType;
  private Object defaultValue;

  public ConductorArgument(Project project, UUID id, Conductor conductor, String name, ValueType valueType, Object defaultValue) {
    super(project, id, name);
    this.conductor = conductor;
    this.valueType = valueType;
    this.defaultValue = defaultValue;
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ConductorArgument getInstanceByName(Conductor conductor, String name) {
    final ConductorArgument[] argument = {null};

    handleWorkDB(conductor.getProject(), workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_CONDUCTOR, KEY_NAME, KEY_TYPE, KEY_DEFAULT_VALUE)
          .where(Sql.Value.and(Sql.Value.equalP(KEY_CONDUCTOR), Sql.Value.equalP(KEY_NAME))).preparedStatement();
        statement.setString(1, conductor.getId());
        statement.setString(2, name);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          ValueType type = ValueType.valueOf(resultSet.getInt(KEY_TYPE));
          Object object = null;
          switch (type) {
            case String:
              object = resultSet.getString(KEY_DEFAULT_VALUE);
              break;
            case Integer:
              object = resultSet.getInt(KEY_DEFAULT_VALUE);
              break;
            case Double:
              object = resultSet.getDouble(KEY_DEFAULT_VALUE);
              break;
            case Boolean:
              object = resultSet.getBoolean(KEY_DEFAULT_VALUE);
              break;
          }
          argument[0] = new ConductorArgument(
            conductor.getProject(),
            UUID.fromString(resultSet.getString(KEY_ID)),
            conductor,
            resultSet.getString(KEY_NAME),
            type,
            object
          );
        }
      }
    });

    return argument[0];
  }

  public static ArrayList<ConductorArgument> getList(Conductor conductor) {
    ArrayList<ConductorArgument> simulatorList = new ArrayList<>();

    handleWorkDB(conductor.getProject(), workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_CONDUCTOR, KEY_NAME, KEY_TYPE, KEY_DEFAULT_VALUE)
          .where(Sql.Value.equalP(KEY_CONDUCTOR)).preparedStatement();
        statement.setString(1, conductor.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          ValueType type = ValueType.valueOf(resultSet.getInt(KEY_TYPE));
          Object object = null;
          switch (type) {
            case String:
              object = resultSet.getString(KEY_DEFAULT_VALUE);
              break;
            case Integer:
              object = resultSet.getInt(KEY_DEFAULT_VALUE);
              break;
            case Double:
              object = resultSet.getDouble(KEY_DEFAULT_VALUE);
              break;
            case Boolean:
              object = resultSet.getBoolean(KEY_DEFAULT_VALUE);
              break;
          }
          simulatorList.add(new ConductorArgument(
            conductor.getProject(),
            UUID.fromString(resultSet.getString(KEY_ID)),
            conductor,
            resultSet.getString(KEY_NAME),
            type,
            object
            ));
        }
      }
    });

    return simulatorList;
  }

  public static ConductorArgument create(Conductor conductor, String name, ValueType type, Object defaultValue) {
    ConductorArgument argument = new ConductorArgument(conductor.getProject(), UUID.randomUUID(), conductor, name, type, defaultValue);

    if (
      handleWorkDB(conductor.getProject(), workUpdater, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into " + TABLE_NAME + "(id,name," +
            KEY_CONDUCTOR + ","+
            KEY_TYPE + ","
            + KEY_DEFAULT_VALUE + ") values(?,?,?,?.?);");
          statement.setString(1, argument.getId());
          statement.setString(2, argument.getName());
          statement.setString(3, conductor.getId());
          statement.setInt(4, type.toInt());
          switch (type) {
            case String:
              statement.setString(5, String.valueOf(defaultValue));
              break;
            case Integer:
              statement.setInt(5, (int) defaultValue);
              break;
            case Double:
              statement.setDouble(5, (double) defaultValue);
              break;
            case Boolean:
              statement.setBoolean(5, (boolean) defaultValue);
              break;
          }
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

    return argument;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  @Override
  protected Updater getMainUpdater() {
    return null;
  }

  @Override
  protected Updater getWorkUpdater() {
    return workUpdater;
  }

  private static Updater workUpdater = new Updater() {
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
            db.execute("create table " + TABLE_NAME + "(" +
              "id,name," + KEY_CONDUCTOR + "," + KEY_TYPE + "," + KEY_DEFAULT_VALUE + "," +
              "timestamp_create timestamp default (DATETIME('now','localtime'))" +
              ");");
          }
        }
      ));
    }
  };
}
