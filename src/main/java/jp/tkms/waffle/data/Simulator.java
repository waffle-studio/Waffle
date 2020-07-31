package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.collector.RubyResultCollector;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.InfoLogMessage;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.extractor.RubyParameterExtractor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jruby.RubyProcess;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

public class Simulator extends ProjectData implements DataDirectory, PropertyFile {
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

  private static final HashMap<String, Simulator> instanceMap = new HashMap<>();

  private String name = null;
  private String simulationCommand = null;
  private String defaultParameters = null;
  private String versionId = null;
  private long lastGitCheckTimestamp = 0;

  public Simulator(Project project, String name) {
    super(project);
    this.name = name;
    instanceMap.put(name, this);
    initialise();
  }

  public String getName() {
    return name;
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(KEY_SIMULATOR);
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_SIMULATOR + Constants.EXT_JSON);
  }

  public static Simulator getInstance(Project project, String name) {
    if (name != null && !name.equals("") && Files.exists(getBaseDirectoryPath(project).resolve(name))) {
      Simulator simulator = instanceMap.get(name);
      if (simulator == null) {
        simulator = new Simulator(project, name);
      }
      return simulator;
    }
    return null;
  }

  public static Simulator find(Project project, String key) {
    return getInstance(project, key);
  }

  public static ArrayList<Simulator> getList(Project project) {
    ArrayList<Simulator> simulatorList = new ArrayList<>();

    for (File file : getBaseDirectoryPath(project).toFile().listFiles()) {
      if (file.isDirectory()) {
        simulatorList.add(getInstance(project, file.getName()));
      }
    }

    return simulatorList;
  }

  public static Simulator create(Project project, String name) {
    name = FileName.removeRestrictedCharacters(name);

    Simulator simulator = getInstance(project, name);
    if (simulator == null) {
      simulator = new Simulator(project, name);
    }

    return simulator;
  }

  private void initialise() {
    try {
      Files.createDirectories(getDirectoryPath());
      Files.createDirectories(getBinDirectory());
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (getSimulationCommand() == null) {
      setSimulatorCommand("");
    }

    if (getExtractorNameList() == null) {
      createExtractor(KEY_COMMAND_ARGUMENTS);
      updateExtractorScript(KEY_COMMAND_ARGUMENTS, ResourceFile.getContents("/default_parameter_extractor.rb"));
    }

    if (getCollectorNameList() == null) {
      createCollector(KEY_OUTPUT_JSON);
      updateCollectorScript(KEY_OUTPUT_JSON, ResourceFile.getContents("/default_result_collector.rb"));
    }

    if (! Files.exists(getDirectoryPath().resolve(".git"))) {
      initializeGit();
    }
  }

  private String initializeGit() {
    synchronized (this) {
      try {
        Path gitPath = getDirectoryPath().resolve(".git");
        if (Files.exists(gitPath)) {
          deleteDirectory(gitPath.toFile());
        }
        Git git = Git.init().setDirectory(getDirectoryPath().toFile()).call();
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial").setAuthor("waffle", "waffle@tkms.jp").call();
        git.branchCreate().setName(KEY_REMOTE).call();
        git.merge().include(git.getRepository().findRef(KEY_MASTER)).setMessage("Merge master").call();
        RevCommit commit = git.log().setMaxCount(1).call().iterator().next();
        versionId = commit.getId().getName();
        git.checkout().setName(KEY_MASTER).call();
        git.close();
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
      return versionId;
    }
  }

  private void deleteDirectory(File file) {
    File[] contents = file.listFiles();
    if (contents != null) {
      for (File f : contents) {
        deleteDirectory(f);
      }
    }
    file.delete();
  }

  public synchronized void updateVersionId() {
    if (lastGitCheckTimestamp + 2000 > System.currentTimeMillis()) {
      lastGitCheckTimestamp = System.currentTimeMillis();
      return;
    }
    synchronized (this) {
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

          git.commit().setMessage((changed.isEmpty() ? "" : "R ") + LocalDateTime.now()).setAuthor("waffle", "waffle@tkms.jp").call();

          if (!changed.isEmpty()) {
            git.checkout().setName(KEY_REMOTE).call();
            git.merge().include(git.getRepository().findRef(KEY_MASTER)).setMessage("Merge master").call();
            git.checkout().setName(KEY_MASTER).call();

            versionId = null;
          }
          git.log().setMaxCount(1).call().forEach(c -> c.getId());
        }
        git.close();
      } catch (GitAPIException | IOException e) {
        ErrorLogMessage.issue(e);

        initializeGit();
      }
      getVersionId();
      lastGitCheckTimestamp = System.currentTimeMillis();
    }
  }

  public String getVersionId() {
    synchronized (this) {
      if (versionId == null) {
        try{
          if (!Files.exists(getDirectoryPath().resolve(".git"))) {
            initializeGit();
          }

          Git git = Git.open(getDirectoryPath().toFile());
          git.checkout().setName(KEY_REMOTE).call();
          RevCommit commit = git.log().setMaxCount(1).call().iterator().next();
          versionId = commit.getId().getName();
          git.checkout().setName(KEY_MASTER).call();
          git.close();
        } catch (Exception e) {
          ErrorLogMessage.issue(e);

          versionId = initializeGit();
        }
      }
      return versionId;
    }
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath(getProject()).resolve(name);
  }

  public Path getBinDirectory() {
    return getDirectoryPath().resolve(KEY_REMOTE).toAbsolutePath();
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
      defaultParameters = new JSONObject(json).toString(2);
      updateFileContents(KEY_DEFAULT_PARAMETERS + Constants.EXT_JSON, defaultParameters);
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
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

    if (! Files.exists(path)) {
      try {
        FileWriter filewriter = new FileWriter(path.toFile());
        filewriter.write(new RubyParameterExtractor().contentsTemplate());
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    putToArrayOfProperty(KEY_EXTRACTOR, name);
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

    if (! Files.exists(path)) {
      try {
        FileWriter filewriter = new FileWriter(path.toFile());
        filewriter.write(new RubyResultCollector().contentsTemplate());
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    putToArrayOfProperty(KEY_COLLECTOR, name);
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

  public SimulatorRun runTest(Host host, String parametersJsonText) {
    String baseRunName = "TESTRUN-" + name;
    RunNode runNode = RunNode.getInstanceByName(getProject(), Paths.get(baseRunName));
    if (runNode == null) {
      runNode = RunNode.getRootInstance(getProject()).createInclusiveRunNode(baseRunName);
    }
    SimulatorRun run = SimulatorRun.create(runNode.createSimulatorRunNode(LocalDateTime.now().toString()), Actor.getRootInstance(getProject()), this, host);
    setToProperty(KEY_TESTRUN, run.getId());
    run.putParametersByJson(parametersJsonText);
    run.start();
    return run;
  }

  public SimulatorRun getLatestTestRun() throws RunNotFoundException {
    return SimulatorRun.getInstance(getProject(), getStringFromProperty(KEY_TESTRUN));
  }

  JSONObject propertyStoreCache = null;
  @Override
  public JSONObject getPropertyStoreCache() {
    return propertyStoreCache;
  }
  @Override
  public void setPropertyStoreCache(JSONObject cache) {
    propertyStoreCache = cache;
  }
}
