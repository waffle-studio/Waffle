package jp.tkms.waffle.data;

import jp.tkms.waffle.data.log.ErrorLogMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class RunNode extends DirectoryBaseData {
  public static final String KEY_RUN = "run";
  Project project;
  Path localPath;

  public RunNode() {
  }

  public RunNode(Project project, Path path) {
    super(path.getFileName().toString());
    this.project = project;
    this.localPath = getBaseDirectoryPath(project).relativize(path);
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath(project).resolve(localPath);
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(KEY_RUN);
  }

  public static RunNode getInstanceByName(Project project, Path path) {
    return new RunNode(project, path);
  }

  public static RunNode getRootInstance(Project project) {
    return new RunNode(project, getBaseDirectoryPath(project));
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
}
