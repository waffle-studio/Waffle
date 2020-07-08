package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.State;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;

public class RunNode implements DataDirectory, EntityProperty {
  public static final String KEY_EXPECTED_NAME = "expected_name";
  public static final String KEY_PROPERTY = "property";
  public static final String KEY_RUN = "run";
  public static final String KEY_STATE = "state";
  public static final String KEY_NOTE_TXT = "note.txt";
  public static final String KEY_ERROR_NOTE_TXT = "error_note.txt";

  protected Workspace workspace;
  protected Path path;

  public RunNode() {
  }

  public RunNode(Workspace workspace, Path path) {
    this.workspace = workspace;
    this.path = path;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public Path getPath() {
    return path;
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_PROPERTY + Constants.EXT_JSON);
  }

  @Override
  public Path getDirectoryPath() {
    return getDirectoryPath(workspace, path);
  }

  public static Path getDirectoryPath(Workspace workspace, Path path) {
    return workspace.getDirectoryPath().resolve(path);
  }

  public Project getProject() {
    return workspace.getProject();
  }

  public String getSimpleName() {
    return getDirectoryPath().getFileName().toString();
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(KEY_RUN);
  }

  public static RunNode getInstance(Workspace workspace, Path path) {
    Path instancePath = path;
    if (path.isAbsolute()) {
      instancePath = workspace.getDirectoryPath().relativize(path);
    }
    Path dirPath = workspace.getDirectoryPath().resolve(instancePath);

    if (Files.exists(dirPath.resolve(ParallelRunNode.KEY_PARALLEL))) {
      return new ParallelRunNode(workspace, instancePath);
    } else if (Files.exists(dirPath.resolve(SimulatorRunNode.KEY_SIMULATOR))) {
      return new SimulatorRunNode(workspace, instancePath);
    } else if (Files.exists(dirPath.resolve(InclusiveRunNode.KEY_INCLUSIVE))) {
      return new InclusiveRunNode(workspace, instancePath);
    }
    return workspace;
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
          list.add(getInstance(workspace, path.toAbsolutePath()));
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
    if (this instanceof Workspace) {
      return null;
    }
    return getInstance(workspace, path.getParent());
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
   Path nodePath = path.resolve(generateUniqueName(name));
    try {
      Files.createDirectories(getDirectoryPath(workspace, nodePath));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new InclusiveRunNode(workspace, nodePath);
  }

  public ParallelRunNode createParallelRunNode(String name) {
    Path nodePath = path.resolve(generateUniqueName(name));
    try {
      Files.createDirectories(getDirectoryPath(workspace, nodePath));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new ParallelRunNode(workspace, nodePath);
  }

  public SimulatorRunNode createSimulatorRunNode(String name) {
    Path nodePath = path.resolve(generateUniqueName(name));
    try {
      Files.createDirectories(getDirectoryPath(workspace, nodePath));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new SimulatorRunNode(workspace, nodePath);
  }

  public ParallelRunNode switchToParallel() {
    try {
      Files.delete(getDirectoryPath().resolve(InclusiveRunNode.KEY_INCLUSIVE));
    } catch (IOException e) {}
    try {
      getDirectoryPath().resolve(ParallelRunNode.KEY_PARALLEL).toFile().createNewFile();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    return (ParallelRunNode) getInstance(workspace, path);
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

    if (Files.exists(getDirectoryPath())) {
      try {
        Path nextPath = path.getParent().resolve(nextName);
        Files.move(getDirectoryPath(), nextPath);
        this.path = workspace.getDirectoryPath().relativize(nextPath);
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

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
