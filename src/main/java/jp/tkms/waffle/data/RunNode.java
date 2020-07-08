package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.State;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;

public class RunNode extends DirectoryBaseData {
  public static final String KEY_EXPECTED_NAME = "expected_name";
  public static final String KEY_PROPERTY = "property";
  public static final String KEY_RUN = "run";
  public static final String KEY_STATE = "state";
  public static final String KEY_NOTE_TXT = "note.txt";
  public static final String KEY_ERROR_NOTE_TXT = "error_note.txt";
  Project project;

  public RunNode() {
  }

  public RunNode(Class clazz, Project project, Path path) {
    super(clazz, path);
    this.project = project;
  }

  @Override
  protected Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_PROPERTY + Constants.EXT_JSON);
  }

  @Override
  public Path getDirectoryPath() {
    return getDirectoryPath(getId()).toAbsolutePath();
  }

  public Project getProject() {
    return project;
  }

  public String getSimpleName() {
    return getDirectoryPath().getFileName().toString();
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(KEY_RUN);
  }

  public static RunNode getInstanceByName(Project project, Path path) {
    Path instancePath = getBaseDirectoryPath(project).resolve(path);
    if (Files.exists(instancePath.resolve(ParallelRunNode.KEY_PARALLEL))) {
      return new ParallelRunNode(project, getBaseDirectoryPath(project).resolve(path));
    } else if (Files.exists(instancePath.resolve(SimulatorRunNode.KEY_SIMULATOR))) {
      return new SimulatorRunNode(project, getBaseDirectoryPath(project).resolve(path));
    } else if (Files.exists(instancePath)) {
      return new InclusiveRunNode(project, getBaseDirectoryPath(project).resolve(path));
    }
    return null;
  }

  public static RunNode getRootInstance(Project project) {
    return new InclusiveRunNode(project, getBaseDirectoryPath(project));
  }

  public static RunNode getInstance(Project project, String id) {
    Path path = getDirectoryPath(id);
    if (path == null) {
      return null;
    }
    return getInstanceByName(project, path.toAbsolutePath());
  }

  public ArrayList<RunNode> getList() {
    ArrayList<RunNode> list = new ArrayList<>();

    try {
      Files.list(getDirectoryPath()).sorted(Comparator.comparingLong(path -> {
        try {
          return Files.readAttributes(path, BasicFileAttributes.class).creationTime().toInstant().toEpochMilli() * -1;
        } catch (IOException e) {
          return 0;
        }
      })).forEach(path -> {
        if (Files.isDirectory(path)) {
          list.add(getInstanceByName(project, path.toAbsolutePath()));
        }
      });
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

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
      resetId(RunNode.class, path);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new InclusiveRunNode(project, path);
  }

  public ParallelRunNode createParallelRunNode(String name) {
    Path path = getDirectoryPath().resolve(generateUniqueName(name));
    try {
      Files.createDirectories(path);
      resetId(RunNode.class, path);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new ParallelRunNode(project, path);
  }

  public SimulatorRunNode createSimulatorRunNode(String name) {
    Path path = getDirectoryPath().resolve(generateUniqueName(name));
    try {
      Files.createDirectories(path);
      resetId(RunNode.class, path);
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

  public String rename(String name) {
    setExpectedName(name);
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
    return nextName;
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
  }

  public String getNote() {
    return getFileContents(KEY_NOTE_TXT);
  }

  public void appendErrorNote(String note) {
    createNewFile(KEY_ERROR_NOTE_TXT);
    updateFileContents(KEY_ERROR_NOTE_TXT, getErrorNote().concat(note).concat("\n"));
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
        ;
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

    setToProperty(KEY_STATE, next.ordinal());
  }

  public State getState() {
    return State.valueOf(getIntFromProperty(KEY_STATE, State.None.ordinal()));
  }
}
