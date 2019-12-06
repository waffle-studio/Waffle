package jp.tkms.waffle.data;

import jp.tkms.waffle.collector.AbstractResultCollector;
import jp.tkms.waffle.collector.JsonResultCollector;
import jp.tkms.waffle.data.util.Sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class ParameterExtractor extends ProjectData {
  protected static final String TABLE_NAME = "parameter_extractor";
  private static final String KEY_SIMULATOR = "simulator";
  private static final String KEY_COLLECTOR_TYPE = "collector_type";
  private static final String KEY_CONTENTS = "contents";

  protected Simulator simulator = null;
  protected AbstractResultCollector resultCollector = null;
  protected String contents = null;

  public ParameterExtractor(Project project) {
    super(project);
  }

  public static ArrayList<String> getResultCollectorNameList() {
    return new ArrayList<>(Arrays.asList(
      JsonResultCollector.class.getCanonicalName()
    ));
  }

  public ParameterExtractor(Project project, UUID id, String name) {
    super(project, id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Simulator getInstance(Project project, String id) {
    final Simulator[] simulator = {null};

    handleDatabase(new ParameterExtractor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          simulator[0] = new Simulator(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return simulator[0];
  }

  public static ArrayList<ParameterExtractor> getList(Simulator simulator) {
    Project project = simulator.getProject();
    ArrayList<ParameterExtractor> collectorList = new ArrayList<>();

    handleDatabase(new ParameterExtractor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME)
          .where(Sql.Value.equalP(KEY_SIMULATOR)).toPreparedStatement();
        statement.setString(1, simulator.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          collectorList.add(new ParameterExtractor(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME))
          );
        }
      }
    });

    return collectorList;
  }

  public static ParameterExtractor create(Simulator simulator, String name, AbstractResultCollector collector, String contents) {
    Project project = simulator.getProject();
    ParameterExtractor resultCollector = new ParameterExtractor(project, UUID.randomUUID(), name);

    handleDatabase(new ParameterExtractor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Insert(db, TABLE_NAME,
          KEY_ID, KEY_NAME, KEY_SIMULATOR, KEY_COLLECTOR_TYPE, KEY_CONTENTS).toPreparedStatement();
        statement.setString(1, resultCollector.getId());
        statement.setString(2, resultCollector.getName());
        statement.setString(3, simulator.getId());
        statement.setString(4, collector.getClass().getCanonicalName());
        statement.setString(5, contents);
        statement.execute();
      }
    });

    return resultCollector;
  }

  public static ParameterExtractor create(Simulator simulator, String name, AbstractResultCollector collector) {
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
      ArrayList<UpdateTask> updateTasks() {
        return new ArrayList<UpdateTask>(Arrays.asList(
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              new Sql.Create(db, TABLE_NAME,
                KEY_ID, KEY_NAME,
                KEY_SIMULATOR,
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
