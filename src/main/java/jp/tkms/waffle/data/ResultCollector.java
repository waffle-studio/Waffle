package jp.tkms.waffle.data;

import jp.tkms.waffle.collector.AbstractResultCollector;
import jp.tkms.waffle.collector.JsonResultCollector;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.submitter.AbstractSubmitter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class ResultCollector extends SimulatorData {
  protected static final String TABLE_NAME = "result_collector";
  private static final String KEY_COLLECTOR_TYPE = "collector_type";
  private static final String KEY_CONTENTS = "contents";

  protected AbstractResultCollector resultCollector = null;
  protected String contents = null;

  public ResultCollector(Simulator simulator) {
    super(simulator);
  }

  public static ArrayList<String> getResultCollectorNameList() {
    return new ArrayList<>(Arrays.asList(
      JsonResultCollector.class.getCanonicalName()
    ));
  }

  public ResultCollector(Simulator simulator, UUID id, String name) {
    super(simulator, id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ResultCollector getInstance(Simulator simulator, String id) {
    final ResultCollector[] collector = {null};

    handleDatabase(new ResultCollector(simulator), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          collector[0] = new ResultCollector(
            simulator,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return collector[0];
  }

  public static ArrayList<ResultCollector> getList(Simulator simulator) {
    ArrayList<ResultCollector> collectorList = new ArrayList<>();

    handleDatabase(new ResultCollector(simulator), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME).executeQuery();
        while (resultSet.next()) {
          collectorList.add(new ResultCollector(
            simulator,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME))
          );
        }
      }
    });

    return collectorList;
  }

  public static ResultCollector create(Simulator simulator, String name, AbstractResultCollector collector, String contents) {
    Project project = simulator.getProject();
    ResultCollector resultCollector = new ResultCollector(simulator, UUID.randomUUID(), name);

    handleDatabase(new ResultCollector(simulator), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Insert(db, TABLE_NAME,
          Sql.Value.equal(KEY_ID, resultCollector.getId()),
          Sql.Value.equal(KEY_NAME, resultCollector.getName()),
          Sql.Value.equal(KEY_COLLECTOR_TYPE, collector.getClass().getCanonicalName()),
          Sql.Value.equal(KEY_CONTENTS, contents)
        ).execute();
      }
    });

    return resultCollector;
  }

  public static ResultCollector create(Simulator simulator, String name, AbstractResultCollector collector) {
    return create(simulator, name, collector, collector.contentsTemplate());
  }

  public AbstractResultCollector getResultCollector() {
    if (resultCollector == null) {
      resultCollector = AbstractResultCollector.getInstance(getFromDB(KEY_COLLECTOR_TYPE));
    }
    return resultCollector;
  }

  public String getContents() {
    if (contents == null) {
      contents = getFromDB(KEY_CONTENTS);
    }
    return contents;
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
              new Sql.Create(db, TABLE_NAME,
                KEY_ID, KEY_NAME,
                KEY_COLLECTOR_TYPE,
                KEY_CONTENTS,
                Sql.Create.timestamp("timestamp_create")
              ).execute();
            }
          }
        ));
      }
    };
  }
}
