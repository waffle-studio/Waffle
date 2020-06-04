package jp.tkms.waffle;

import jp.tkms.waffle.component.ProjectsComponent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {
  static final public String APP_NAME = "WAFFLE";
  static final public String APP_FULL_NAME = "Workflow Administration Framework to Facilitate Lucid Exploration";
  static final public String MAIN_DB_NAME = ".main.db";
  static final public String PROJECT_DB_NAME = ".project.db";
  static final public String ROOT_PAGE = ProjectsComponent.getUrl();
  static final public Path WORK_DIR = Paths.get("." + File.separator + APP_NAME.toLowerCase());
  static final public String PROJECT = "project";
  static final public String CONDUCTOR_TEMPLATE = "conductor-template";
  static final public String LISTENER_TEMPLATE = "listener-template";
  static final public String LOCAL_WORK_DIR = "tmp";
  static final public String LOCAL_XSUB_DIR = "./xsub";
  public static final String EXT_JSON = ".json";
  public static final String EXT_RUBY = ".rb";
}
