package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class RunNode extends DirectoryBaseData {
  public static final String KEY_TYPE = "type";
  public static final String KEY_RUN = "run";
  Project project;
  Path absolutePath;
  Path localPath;

  public RunNode() {
  }

  public RunNode(Project project, Path path) {
    super(getBaseDirectoryPath(project).relativize(path).toString());
    this.project = project;
    this.absolutePath = path.toAbsolutePath();
    this.localPath = getBaseDirectoryPath(project).relativize(path);
  }

  @Override
  protected Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_RUN + Constants.EXT_JSON);
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath(project).resolve(".").resolve(localPath);
  }

  public String getSimpleName() {
    return absolutePath.getFileName().toString();
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(KEY_RUN);
  }

  public static RunNode getInstanceByName(Project project, Path path) {
    Path instancePath = getBaseDirectoryPath(project).resolve(path);
    if (Files.exists(instancePath.resolve(ParallelRunNode.KEY_PARALLEL))) {
      return new ParallelRunNode(project, getBaseDirectoryPath(project).resolve(path));
    }
    return new RunNode(project, getBaseDirectoryPath(project).resolve(path));
  }

  public static RunNode getRootInstance(Project project) {
    return new RunNode(project, getBaseDirectoryPath(project));
  }

  public static RunNode getInstance(Project project, String id) {
    return getInstanceByName(project, getDirectory(id).toAbsolutePath());
  }

  public ArrayList<RunNode> getList() {
    ArrayList<RunNode> list = new ArrayList<>();

    try {
      Files.list(getDirectoryPath()).forEach(path -> {
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
    try {
      if (Files.isSameFile(getBaseDirectoryPath(project), absolutePath)) {
        return null;
      }
    } catch (IOException e) { }

    return getInstanceByName(project, Paths.get(".").resolve(localPath).getParent());
  }

  public InclusiveRunNode createInclusiveRunNode(String name) {
    Path path = absolutePath.resolve(name);
    try {
      Files.createDirectories(path);
      resetUuid(path);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new InclusiveRunNode(project, path);
  }

  public ParallelRunNode createParallelRunNode(String name) {
    Path path = absolutePath.resolve(name);
    try {
      Files.createDirectories(path);
      resetUuid(path);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new ParallelRunNode(project, path);
  }

  public ParallelRunNode switchToParallel() {
    return new ParallelRunNode(project, absolutePath);
  }

  public boolean isRoot() {
    return getParent() == null;
  }
}
