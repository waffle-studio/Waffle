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
  static final public String SIMULATOR_DB_NAME = ".simulator.db";
  static final public String ROOT_PAGE = ProjectsComponent.getUrl();
  static final public String WORK_DIR = "." + File.separator + APP_NAME.toLowerCase();
  static final public String DEFAULT_PROJECT_DIR = WORK_DIR + File.separator + "project" + File.separator + "${NAME}";
  static final public String CONDUCTOR_TEMPLATE_DIR = WORK_DIR + File.separator + "conductor-template" + File.separator + "${NAME}";
  static final public String LISTENER_TEMPLATE_DIR = WORK_DIR + File.separator + "listener-template" + File.separator + "${NAME}";
  static final public String LOCAL_WORK_DIR = "tmp";
  static final public String LOCAL_XSUB_DIR = "./xsub";
}
