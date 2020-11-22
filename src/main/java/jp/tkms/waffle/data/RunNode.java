package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.FileBuffer;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.InstanceCache;
import jp.tkms.waffle.data.util.State;
import org.ehcache.Cache;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

public class RunNode implements DataDirectory, PropertyFile, InternalHashedLinkData {
  public static final String KEY_EXPECTED_NAME = "expected_name";
  public static final String KEY_PROPERTY = "property";
  public static final String KEY_RUN = "run";
  public static final String KEY_STATE = "state";
  public static final String KEY_NOTE_TXT = "note.txt";
  public static final String KEY_ERROR_NOTE_TXT = "error_note.txt";
  protected static final String KEY_TMP = ".tmp";
  protected static final String KEY_SORT_INDEX = ".SORT_INDEX";

  //private static final Cache<String, RunNode> instanceCache = new InstanceCache<RunNode>(RunNode.class, 1000).getCacheStore();
  //private static final HashMap<String, RunNode> instanceMap = new HashMap<>();

  Project project;
  Path path;
  UUID id;
  Long sortIndex;
  String note;
  String simpleName;
  State state;

  public RunNode(Project project, Path path) {
    this.project = project;
    this.id = UUID.fromString(getDataId(path));
    this.path = getDataDirectory(id);

    //instanceCache.put(path.toString(), this);

    initialize();
  }

  public String getName() {
    return path.getFileName().toString();
  }

  public void initialize() {

    Path sortIndexPath = path.resolve(KEY_SORT_INDEX);
    if (Files.exists(sortIndexPath)) {
      try {
        sortIndex = Long.valueOf(getFileContents(sortIndexPath));
      } catch (Exception e) {}
    }
    if (sortIndex == null) {
      createNewFile(sortIndexPath);
      sortIndex = Long.valueOf(0);
      try {
        sortIndex = Files.readAttributes(path, BasicFileAttributes.class).creationTime().toInstant().toEpochMilli();
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
      updateFileContents(sortIndexPath, String.valueOf(sortIndex));
    }
  }

  public String getId() {
    return id.toString();
  }

  public UUID getUuid() {
    return id;
  }

  public Long getSortIndex() {
    return sortIndex;
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_PROPERTY + Constants.EXT_JSON);
  }

  @Override
  public Path getDirectoryPath() {
    return path.toAbsolutePath();
  }

  public Project getProject() {
    return project;
  }

  @Override
  public String getInternalDataGroup() {
    return RunNode.class.getName();
  }

  public String getSimpleName() {
    if (simpleName == null) {
      simpleName = getDirectoryPath().getFileName().toString();
    }
    return simpleName;
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(KEY_RUN);
  }

  public static RunNode getInstanceByName(Project project, Path path) {
    Path instancePath = getBaseDirectoryPath(project).resolve(path);
    RunNode runNode = null;
    if (Files.exists(instancePath.resolve(ParallelRunNode.KEY_PARALLEL))) {
      //runNode = instanceCache.get(instancePath.toString());
      if (runNode == null) {
        runNode = new ParallelRunNode(project, getBaseDirectoryPath(project).resolve(path));
      }
    } else if (Files.exists(instancePath.resolve(SimulatorRunNode.KEY_SIMULATOR))) {
      //runNode = instanceCache.get(instancePath.toString());
      if (runNode == null) {
        runNode = new SimulatorRunNode(project, getBaseDirectoryPath(project).resolve(path));
      }
    } else if (Files.exists(instancePath)) {
      //runNode = instanceCache.get(instancePath.toString());
      if (runNode == null) {
        runNode = new InclusiveRunNode(project, getBaseDirectoryPath(project).resolve(path));
      }
    }
    return runNode;
  }

  public static RunNode getRootInstance(Project project) {
    Path path = getBaseDirectoryPath(project);
    RunNode runNode = getInstanceByName(project, path);
    if (runNode == null) {
      runNode = new InclusiveRunNode(project, path);
    }
    return runNode;
  }

  public static RunNode getInstance(Project project, String id) {
    Path path = InternalHashedLinkData.getDataPath(project, RunNode.class.getName(), id);
    if (path == null) {
      return null;
    }
    return getInstanceByName(project, path.toAbsolutePath());
  }

  public ArrayList<RunNode> getList() {
    ArrayList<RunNode> list = new ArrayList<>();
    /*
    try {
      Files.list(getDirectoryPath()).sorted(
        Comparator.comparingLong(path -> {
        try {
          return Files.readAttributes(path, BasicFileAttributes.class).creationTime().toInstant().toEpochMilli() * -1;
        } catch (IOException e) {
          return 0;
        }
      })
      ).forEach(path -> {
        if (Files.isDirectory(path)) {
          list.add(getInstance(project, path.toAbsolutePath()));
        }
      });
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
     */

    for (File file : getDirectoryPath().toFile().listFiles()) {
      if (file.isDirectory()) {
        list.add(getInstanceByName(project, file.toPath().toAbsolutePath()));
      }
    }

    list.sort(Comparator.comparingLong(RunNode::getSortIndex).reversed());

    return list;
  }

  public static ArrayList<RunNode> getList(RunNode node) {
    return node.getList();
  }

  public RunNode getParent() {
    Path path = getDirectoryPath();
    try {
      if (Files.isSameFile(getBaseDirectoryPath(project), path)) {
        return null;
      }
    } catch (IOException e) { }

    return getInstanceByName(project, Paths.get(".").resolve( getBaseDirectoryPath(project).relativize(path) ).getParent());
  }

  private String generateUniqueName(String name) {
    name = FileName.removeRestrictedCharacters(name);
    String result = name;
    int count = 0;
    Path path = getDirectoryPath();
    while (result.length() <= 0 || Files.exists(path.resolve(result))) {
      result = name + '_' + count++;
      //name = (name.length() > 0 ? "_" : "") + UUID.randomUUID().toString().replaceFirst("-.*$", "");
    }
    return result;
  }

  public InclusiveRunNode createInclusiveRunNode(String name) {
    Path path = getDirectoryPath().resolve(generateUniqueName(name));
    try {
      Files.createDirectories(path);
      InternalHashedLinkData.resetDataId(path);
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    return new InclusiveRunNode(project, path);
  }

  public ParallelRunNode createParallelRunNode(String name) {
    Path path = getDirectoryPath().resolve(generateUniqueName(name));
    try {
      Files.createDirectories(path);
      InternalHashedLinkData.resetDataId(path);
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    return new ParallelRunNode(project, path);
  }

  public SimulatorRunNode createSimulatorRunNode(String name) {
    Path path = getDirectoryPath().resolve(generateUniqueName(name));
    try {
      Files.createDirectories(path);
      InternalHashedLinkData.resetDataId(path);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new SimulatorRunNode(project, path);
  }

  public ParallelRunNode switchToParallel() {
    return new ParallelRunNode(project, getDirectoryPath());
  }

  public boolean isRoot() {
    return getParent() == null;
  }

  public InclusiveRunNode moveToVirtualNode() {
    String virtualNodeName = getDirectoryPath().getFileName().toString();
    RunNode parent = getParent();
    String name = FileName.removeRestrictedCharacters(getExpectedName());
    if (name.length() <= 0) {
      name = "_0";
    }
    rename(KEY_TMP, false);
    InclusiveRunNode node = parent.createInclusiveRunNode(virtualNodeName);

    try {
      BasicFileAttributeView nodeAttributes = Files.getFileAttributeView(node.getDirectoryPath(), BasicFileAttributeView.class);
      FileTime time = Files.readAttributes(getDirectoryPath(), BasicFileAttributes.class).creationTime();
      nodeAttributes.setTimes(time, time, time);
    } catch (IOException e) { }

    replace(node.getDirectoryPath().resolve(name));
    return node;
  }

  public String rename(String name, boolean isExpectedName) {
    if (isExpectedName) {
      setExpectedName(name);
    }
    name = FileName.removeRestrictedCharacters(name);
    String nextName = name;
    int count = 0;
    Path path = getDirectoryPath();
    if (nextName.length() > 0) {
      count += 1;
    }
    while (nextName.length() <= 0 || Files.exists(path.getParent().resolve(nextName))) {
      nextName = name + '_' + count++;
    }
    replace(path.getParent().resolve(nextName));
    simpleName = null;
    return nextName;
  }

  public void replace(Path path) {
    int count = 1;
    while (Files.exists(path)) {
      path = path.getParent().resolve(path.getFileName().toString() + '_' + count++);
      //name = (name.length() > 0 ? "_" : "") + UUID.randomUUID().toString().replaceFirst("-.*$", "");
    }
    Path localPath = Constants.WORK_DIR.relativize(path.toAbsolutePath());
    String id = getId();

    if (Files.exists(getDirectoryPath())) {
      try {
        Files.move(getDirectoryPath(), path);
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    InternalHashedLinkData.updateDataPath(getProject(), getInternalDataGroup(), id, localPath);
    this.path = path;
    //instanceCache.remove(this.path.toString());
  }

  public String rename(String name) {
    return rename(name, true);
  }

  private void setExpectedName(String name) {
    setToProperty(KEY_EXPECTED_NAME, name);
  }

  public String getExpectedName() {
    String name = getStringFromProperty(KEY_EXPECTED_NAME);
    if (name == null) {
      name = getSimpleName();
    }
    return name;
  }

  public void setNote(String text) {
    createNewFile(KEY_NOTE_TXT);
    updateFileContents(KEY_NOTE_TXT, text);
    note = text;
  }

  public String getNote() {
    if (note == null) {
      note = getFileContents(KEY_NOTE_TXT);
    }
    return note;
  }

  public void appendErrorNote(String note) {
    if (note != null) {
      createNewFile(KEY_ERROR_NOTE_TXT);
      updateFileContents(KEY_ERROR_NOTE_TXT, getErrorNote().concat(note).concat("\n"));
    }
  }

  public String getErrorNote() {
    return getFileContents(KEY_ERROR_NOTE_TXT);
  }

  protected void propagateState(State prev, State next) {
    if (! (this instanceof SimulatorRunNode)) {
      if (prev != null) {
        int num = getIntFromProperty(prev.name(), 0);
        if (num > 0) {
          num -= 1;
        }
        setToProperty(prev.name(), num);
      }

      if (next != null) {
        setToProperty(next.name(),
          getIntFromProperty(next.name(), 0)
            + 1);
      }
    }

    if (! isRoot()) {
      getParent().propagateState(prev, next);
    }

    setState(next);
  }

  private void setState(State state) {
    this.state = state;
    setToProperty(KEY_STATE, state.ordinal());
  }

  public State getState() {
    if (state == null) {
      state = State.valueOf(getIntFromProperty(KEY_STATE, State.None.ordinal()));
    }
    return state;
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
