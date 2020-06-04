package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.collector.RubyResultCollector;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.extractor.RubyParameterExtractor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class Simulator extends ProjectData implements DataDirectory {
  public static final String KEY_SIMULATOR = "simulator";
  public static final String KEY_EXTRACTOR = "extractor";
  public static final String KEY_COMMAND_ARGUMENTS = "command arguments";
  public static final String KEY_COLLECTOR = "collector";
  public static final String KEY_OUTPUT_JSON = "_output.json";
  private static final String KEY_DEFAULT_PARAMETERS = "default_parameters";
  public static final String KEY_TESTRUN = "testrun";

  public static final String KEY_MASTER = "master";
  public static final String KEY_REMOTE = "REMOTE";

  protected static final String TABLE_NAME = "simulator";
  private static final String KEY_SIMULATION_COMMAND = "simulation_command";

  private String simulationCommand = null;
  private String defaultParameters = null;

  public Simulator(Project project, UUID id, String name) {
    super(project, id, name);
  }

  public Simulator(Project project) {
    super(project);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(KEY_SIMULATOR);
  }

  @Override
  protected Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_SIMULATOR + Constants.EXT_JSON);
  }

  public static Simulator getInstance(Project project, String id) {
    final Simulator[] simulator = {null};

    handleDatabase(new Simulator(project), new Handler() {
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

  public static Simulator getInstanceByName(Project project, String name) {
    final Simulator[] simulator = {null};

    handleDatabase(new Simulator(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where name=?;");
        statement.setString(1, name);
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

    if (simulator[0] == null && Files.exists(project.getSimulatorDirectoryPath().resolve(name))) {
      simulator[0] = create(project, name);
    }

    return simulator[0];
  }

  public static Simulator find(Project project, String key) {
    if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(project, key);
    }
    return getInstanceByName(project, key);
  }

  public static ArrayList<Simulator> getList(Project project) {
    ArrayList<Simulator> simulatorList = new ArrayList<>();

    try {
      Files.list(project.getSimulatorDirectoryPath()).forEach(path -> {
        if (Files.isDirectory(path)) {
          simulatorList.add(getInstanceByName(project, path.getFileName().toString()));
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }

    /*
    handleDatabase(new Simulator(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          simulatorList.add(new Simulator(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name"))
          );
        }
      }
    });
    */

    return simulatorList;
  }

  public static Simulator create(Project project, String name) {
    Simulator simulator = new Simulator(project, UUID.randomUUID(), name);

    if (
      handleDatabase(new Simulator(project), new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into " + TABLE_NAME + "(id,name"
            + ") values(?,?);");
          statement.setString(1, simulator.getId());
          statement.setString(2, simulator.getName());
          statement.execute();
        }
      })
    ) {
      try {
        Files.createDirectories(simulator.getDirectoryPath());
        Files.createDirectories(simulator.getBinDirectory());
      } catch (IOException e) {
        e.printStackTrace();
      }

      if (simulator.getSimulationCommand() == null) {
        simulator.setSimulatorCommand("");
      }

      if (simulator.getExtractorNameList() == null) {
        simulator.createExtractor(KEY_COMMAND_ARGUMENTS);
        simulator.updateExtractorScript(KEY_COMMAND_ARGUMENTS, ResourceFile.getContents("/default_parameter_extractor.rb"));
      }

      if (simulator.getCollectorNameList() == null) {
        simulator.createCollector(KEY_OUTPUT_JSON);
        simulator.updateCollectorScript(KEY_OUTPUT_JSON, ResourceFile.getContents("/default_result_collector.rb"));
      }

      if (! Files.exists(simulator.getDirectoryPath().resolve(".git"))) {
        try {
          Git git = Git.init().setDirectory(simulator.getDirectoryPath().toFile()).call();
          git.add().addFilepattern(".").call();
          git.commit().setMessage("Initial").setAuthor("waffle", "waffle@tkms.jp").call();
          git.branchCreate().setName(KEY_REMOTE).call();
          git.checkout().setName(KEY_MASTER).call();
        } catch (GitAPIException e) {
          e.printStackTrace();
        }
      }
    }

    return simulator;
  }

  public void update() {
    try{
      Git git = Git.open(getDirectoryPath().toFile());
      git.add().addFilepattern(".").call();

      for (String missing : git.status().call().getMissing()) {
        git.rm().addFilepattern(missing).call();
      }

      if (!git.status().call().isClean()) {
        Set<String> changed = new HashSet<>();
        changed.addAll(git.status().addPath(KEY_REMOTE).call().getAdded());
        changed.addAll(git.status().addPath(KEY_REMOTE).call().getModified());
        changed.addAll(git.status().addPath(KEY_REMOTE).call().getRemoved());
        changed.addAll(git.status().addPath(KEY_REMOTE).call().getChanged());

        git.commit().setMessage((changed.isEmpty()?"":"R ") + LocalDateTime.now()).setAuthor("waffle","waffle@tkms.jp").call();

        if (!changed.isEmpty()) {
          git.checkout().setName(KEY_REMOTE).call();
          git.merge().include(git.getRepository().findRef(KEY_MASTER)).setMessage("Merge master").call();
          git.checkout().setName(KEY_MASTER).call();
        }
      }
    } catch (GitAPIException | IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Path getDirectoryPath() {
    return getProject().getSimulatorDirectoryPath().resolve(name);
  }

  public Path getBinDirectory() {
    return getDirectoryPath().resolve(KEY_REMOTE);
  }

  public String getSimulationCommand() {
    try {
      if (simulationCommand == null) {
        simulationCommand = getStringFromProperty(KEY_SIMULATION_COMMAND);
      }
    } catch (Exception e) {}
    return simulationCommand;
  }

  public void setSimulatorCommand(String command) {
    simulationCommand = command;
    setToProperty(KEY_SIMULATION_COMMAND, simulationCommand);
  }

  public JSONObject getDefaultParameters() {
    if (defaultParameters == null) {
      defaultParameters = getFileContents(KEY_DEFAULT_PARAMETERS + Constants.EXT_JSON);
      if (defaultParameters.equals("")) {
        defaultParameters = "{}";
        createNewFile(KEY_DEFAULT_PARAMETERS + Constants.EXT_JSON);
        updateFileContents(KEY_DEFAULT_PARAMETERS + Constants.EXT_JSON, defaultParameters);
      }
    }
    return new JSONObject(defaultParameters);
  }

  public void setDefaultParameters(String json) {
    try {
      JSONObject object = new JSONObject(json);
      updateFileContents(KEY_DEFAULT_PARAMETERS + Constants.EXT_JSON, object.toString(2));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Path getExtractorScriptPath(String name) {
    return getDirectoryPath().resolve(KEY_EXTRACTOR).resolve(name + Constants.EXT_RUBY).toAbsolutePath();
  }

  public void createExtractor(String name) {
    Path path = getExtractorScriptPath(name);
    Path dirPath = path.getParent();
    if (! Files.exists(dirPath)) {
      try {
        Files.createDirectories(dirPath);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      FileWriter filewriter = new FileWriter(path.toFile());
      filewriter.write(new RubyParameterExtractor().contentsTemplate());
      filewriter.close();

      putToArrayOfProperty(KEY_EXTRACTOR, name);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void removeExtractor(String name) {
    removeFromArrayOfProperty(KEY_EXTRACTOR, name);
  }

  public void updateExtractorScript(String name, String script) {
    Path path = getExtractorScriptPath(name);
    if (Files.exists(path)) {
      try {
        FileWriter filewriter = new FileWriter(path.toFile());
        filewriter.write(script);
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public String getExtractorScript(String name) {
    String script = "";

    Path path = getExtractorScriptPath(name);
    if (Files.exists(path)) {
      try {
        script = new String(Files.readAllBytes(path));
      } catch (IOException e) {
      }
    }

    return script;
  }

  public List<String> getExtractorNameList() {
    List<String> list = null;
    try {
      JSONArray array = getArrayFromProperty(KEY_EXTRACTOR);
      list = Arrays.asList(array.toList().toArray(new String[array.toList().size()]));
      for (String name : list) {
        if (! Files.exists(getExtractorScriptPath(name))) {
          removeFromArrayOfProperty(KEY_EXTRACTOR, name);
        }
      }
    } catch (JSONException e) {
    }
    return list;
  }

  public List<String> getCollectorNameList() {
    List<String> list = null;
    try {
      JSONArray array = getArrayFromProperty(KEY_COLLECTOR);
      list = Arrays.asList(array.toList().toArray(new String[array.toList().size()]));
      for (String name : list) {
        if (! Files.exists(getCollectorScriptPath(name))) {
          removeFromArrayOfProperty(KEY_COLLECTOR, name);
        }
      }
    } catch (JSONException e) {
    }
    return list;
  }

  public Path getCollectorScriptPath(String name) {
    return getDirectoryPath().resolve(KEY_COLLECTOR).resolve(name + Constants.EXT_RUBY).toAbsolutePath();
  }

  public void createCollector(String name) {
    Path path = getCollectorScriptPath(name);
    Path dirPath = getCollectorScriptPath(name).getParent();
    if (! Files.exists(dirPath)) {
      try {
        Files.createDirectories(dirPath);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      FileWriter filewriter = new FileWriter(path.toFile());
      filewriter.write(new RubyResultCollector().contentsTemplate());
      filewriter.close();

      putToArrayOfProperty(KEY_COLLECTOR, name);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void removeCollector(String name) {
    removeFromArrayOfProperty(KEY_COLLECTOR, name);
  }

  public void updateCollectorScript(String name, String script) {
    Path path = getCollectorScriptPath(name);
    if (Files.exists(path)) {
      try {
        FileWriter filewriter = new FileWriter(path.toFile());
        filewriter.write(script);
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public String getCollectorScript(String name) {
    String script = "";

    Path path = getCollectorScriptPath(name);
    if (Files.exists(path)) {
      try {
        script = new String(Files.readAllBytes(path));
      } catch (IOException e) {
      }
    }

    return script;
  }

  public void runTest(Host host, String parametersJsonText) {
    String baseRunName = "TESTRUN-" + name;
    ConductorRun baseRun = ConductorRun.getInstanceByName(getProject(), baseRunName);
    if (baseRun == null) {
      baseRun = ConductorRun.create(getProject(), ConductorRun.getRootInstance(getProject()), null);
      baseRun.setName(baseRunName);
    }
    SimulatorRun run = SimulatorRun.create(baseRun, this, host);
    setToDB(KEY_TESTRUN, run.getId());
    run.putParametersByJson(parametersJsonText);
    run.start();
  }

  public SimulatorRun getLatestTestRun() {
    return SimulatorRun.getInstance(getProject(), getStringFromDB(KEY_TESTRUN));
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
              db.execute("create table " + TABLE_NAME + "(" +
                "id,name," + KEY_TESTRUN + " default ''," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }
}
