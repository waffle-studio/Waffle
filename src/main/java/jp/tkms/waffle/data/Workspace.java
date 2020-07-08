package jp.tkms.waffle.data;

import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.State;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import static jp.tkms.waffle.data.AbstractRun.*;
import static jp.tkms.waffle.data.Actor.KEY_ACTOR;

public class Workspace extends RunNode {
  private static final String WORKSPACE_DATABASE_FILE = "work.db";
  private static final String KEY_ID = "id";
  private static final String KEY_NAME = "name";
  private static final String KEY_TYPE = "type";

  private Project project;
  private String name;
  private Database database;

  private Workspace(Project project, String name) {
    this.project = project;
    this.name = name;
    this.workspace = this;
    this.path = Paths.get(".");

    createDirectories(this.getDirectoryPath());
    database = Database.getDatabase(this.getDirectoryPath().resolve(WORKSPACE_DATABASE_FILE));

    synchronized (database) {
      try {
        new Sql.Create(database, KEY_ACTOR,
          KEY_ID, KEY_NAME, KEY_TYPE,
          KEY_PARENT, KEY_RESPONSIBLE_ACTOR,
          Sql.Create.withDefault(KEY_FINALIZER, "'[]'"),
          Sql.Create.withDefault(KEY_VARIABLES, "'{}'"),
          Sql.Create.withDefault(KEY_STATE, String.valueOf(State.Created.ordinal())),
          KEY_RUNNODE).execute();
      } catch (SQLException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }

  public static Workspace getInstanceByName(Project project, String name) {
    return new Workspace(project, name);
  }

  public String getName() {
    return name;
  }

  @Override
  public Project getProject() {
    return project;
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath(project).resolve(name);
  }

  public Database getDatabase() {
    return database;
  }
}
