package jp.tkms.waffle;

import jp.tkms.waffle.component.ProjectsComponent;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Environment {
  static final public String APP_NAME = "Waffle";
  static final public String MAIN_DB_NAME = APP_NAME.toLowerCase() + "-main.db";
  static final public String WORK_DB_NAME = APP_NAME.toLowerCase() + "-work.db";
  static final public String ROOT_PAGE = ProjectsComponent.getUrl();
  static final public String DEFAULT_WD = "." + File.separator + "work" + File.separator + "${NAME}";
  static final public String LOCAL_WORK_DIR = "tmp";
  static final public String LOCAL_XSUB_DIR = "xsub";
}
