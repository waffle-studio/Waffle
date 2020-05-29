package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.Sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class ParameterExtractor extends SimulatorData {
  protected static final String TABLE_NAME = "parameter_extractor";
  private static final String KEY_SCRIPT = "script";

  protected String script = null;

  public ParameterExtractor(Simulator simulator) {
    super(simulator);
  }

  public ParameterExtractor(Simulator simulator, UUID id, String name) {
    super(simulator, id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ParameterExtractor getInstance(Simulator simulator, String id) {
    final ParameterExtractor[] extractor = {null};

    handleDatabase(new ParameterExtractor(simulator), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          extractor[0] = new ParameterExtractor(
            simulator,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return extractor[0];
  }

  public static ArrayList<ParameterExtractor> getList(Simulator simulator) {
    ArrayList<ParameterExtractor> collectorList = new ArrayList<>();

    handleDatabase(new ParameterExtractor(simulator), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet =
          new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME).executeQuery();
        while (resultSet.next()) {
          collectorList.add(new ParameterExtractor(
            simulator,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME))
          );
        }
      }
    });

    return collectorList;
  }

  public static ParameterExtractor create(Simulator simulator, String name, String script) {
    ParameterExtractor extractor = new ParameterExtractor(simulator, UUID.randomUUID(), name);

    handleDatabase(new ParameterExtractor(simulator), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Insert(db, TABLE_NAME,
          Sql.Value.equal(KEY_ID, extractor.getId()),
          Sql.Value.equal(KEY_NAME, extractor.getName()),
          Sql.Value.equal(KEY_SCRIPT, script)
        ).execute();
      }
    });

    return extractor;
  }

  public static ParameterExtractor create(Simulator simulator, String name) {
    return create(simulator, name, "");
  }

  public void update(String name, String script) {
    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Update(db, TABLE_NAME,
          Sql.Value.equal(KEY_NAME, name),
          Sql.Value.equal(KEY_SCRIPT, script)
        ).where(Sql.Value.equal(KEY_ID, getId())).execute();
      }
    });
  }

  public String getScript() {
    if (script == null) {
      script = getStringFromDB(KEY_SCRIPT);
    }
    return script;
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
                KEY_ID, KEY_NAME,
                KEY_SCRIPT,
                Sql.Create.timestamp("timestamp_create")
              ).execute();
            }
          }
        ));
      }
    };
  }
}
