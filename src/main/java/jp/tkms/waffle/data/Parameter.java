package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.data.util.Sql;
import org.jruby.Ruby;
import org.jruby.embed.ScriptingContainer;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Parameter extends SimulatorData {
  protected static final String TABLE_NAME = "parameter_model";
  private static final String KEY_PARENT = "parent";
  private static final String KEY_IS_QUANTITATIVE = "quantitative";
  private static final String KEY_DEFAULT_VALUE = "default_value";
  private static final String KEY_DEFAULT_VALUE_UPDATE_SCRIPT = "default_updater";

  private ParameterGroup parent = null;
  private Boolean isQuantitative = null;
  private String defaultValue = null;
  private String defaultValueUpdateScript = null;

  public Parameter(Simulator simulator) {
    super(simulator);
  }

  public Parameter(Simulator simulator, UUID id, String name) {
    super(simulator, id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Parameter getInstance(Simulator simulator, String id) {
    final Parameter[] extractor = {null};

    handleDatabase(new Parameter(simulator), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          extractor[0] = new Parameter(
            simulator,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return extractor[0];
  }

  public static ArrayList<Parameter> getList(ParameterGroup parent) {
    ArrayList<Parameter> collectorList = new ArrayList<>();

    handleDatabase(new Parameter(parent.getSimulator()), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME).where(Sql.Value.equalP(KEY_PARENT)).toPreparedStatement();
        statement.setString(1, parent.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          collectorList.add(new Parameter(
            parent.getSimulator(),
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME))
          );
        }
      }
    });

    return collectorList;
  }

  public static Parameter create(ParameterGroup parent, String name) {
    Parameter parameter = new Parameter(parent.getSimulator(), UUID.randomUUID(), name);

    handleDatabase(new Parameter(parent.getSimulator()), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Insert(db, TABLE_NAME,
          KEY_ID, KEY_NAME, KEY_PARENT).toPreparedStatement();
        statement.setString(1, parameter.getId());
        statement.setString(2, parameter.getName());
        statement.setString(3, parent.getId());
        statement.execute();
        statement = new Sql.Update(db, TABLE_NAME, KEY_DEFAULT_VALUE_UPDATE_SCRIPT).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, defaultUpdateScriptTemplate());
        statement.setString(2, parameter.getId());
        statement.execute();
      }
    });

    return parameter;
  }

  public ParameterGroup getParent() {
    if (parent == null) {
      parent = ParameterGroup.getInstance(getSimulator(), getFromDB(KEY_PARENT));
    }
    return parent;
  }

  public boolean isQuantitative() {
    if (isQuantitative == null) {
      isQuantitative = Boolean.valueOf( getFromDB(KEY_IS_QUANTITATIVE) );
    }
    return isQuantitative;
  }

  public boolean isQuantitative(boolean b) {
    if (handleDatabase((new Parameter(getSimulator())), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("update " + getTableName() + " set " + KEY_IS_QUANTITATIVE + "=? where " + KEY_ID + "=?;");
        statement.setString(1, Boolean.toString(b));
        statement.setString(2, getId());
        statement.execute();
      }
    })) {
      isQuantitative = Boolean.valueOf( b );
    }
    return isQuantitative;
  }

  public String getDefaultValue() {
    if (defaultValue == null) {
      defaultValue = getFromDB(KEY_DEFAULT_VALUE);
    }
    return defaultValue;
  }

  public void setDefaultValue(String value) {
    if (handleDatabase((new Parameter(getSimulator())), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Update(db, getTableName(), KEY_DEFAULT_VALUE).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, value);
        statement.setString(2, getId());
        statement.execute();
      }
    })) {
      defaultValue = value;
    }
  }

  public String getDefaultValueUpdateScript() {
    if (defaultValueUpdateScript == null) {
      defaultValueUpdateScript = getFromDB(KEY_DEFAULT_VALUE_UPDATE_SCRIPT);
    }
    return defaultValueUpdateScript;
  }

  public void setDefaultValueUpdateScript(String script) {
    if (handleDatabase((new Parameter(getSimulator())), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Update(db, getTableName(), KEY_DEFAULT_VALUE_UPDATE_SCRIPT).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, script);
        statement.setString(2, getId());
        statement.execute();
      }
    })) {
      defaultValueUpdateScript = script;
    }
  }

  public void updateDefaultValue(ConductorEntity conductorEntity, JSONObject defaultParameters) {
    Object defaultValue = defaultParameters.get(getName());

    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(getInitScript());
      container.runScriptlet(getDefaultValueUpdateScript());
      defaultValue = container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_update_value", conductorEntity, defaultValue);
    } catch (Exception e) {
      e.printStackTrace();
      BrowserMessage.addMessage("toastr.error('update_value: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }

    defaultParameters.put(getName(), defaultValue);
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
              new Sql.Create(db, TABLE_NAME,
                KEY_ID, KEY_NAME, KEY_PARENT,
                Sql.Create.withDefault(KEY_IS_QUANTITATIVE, "'false'"),
                Sql.Create.withDefault(KEY_DEFAULT_VALUE, "'0'"),
                Sql.Create.withDefault(KEY_DEFAULT_VALUE_UPDATE_SCRIPT, "''"),
                Sql.Create.timestamp("timestamp_create")
              ).execute();
            }
          }
        ));
      }
    };
  }

  private static String defaultUpdateScriptTemplate() {
    return "def update_value(value, registry)\n    return value\nend";
  }
  private String getInitScript() {
    return ResourceFile.getContents("/ruby_init.rb");
  }
}
