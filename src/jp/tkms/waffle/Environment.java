package jp.tkms.waffle;

import jp.tkms.waffle.component.ProjectsComponent;

import java.io.File;

public class Environment {
    static final public String APP_NAME = "Waffle";
    static final public String MAIN_DB_NAME = APP_NAME.toLowerCase() + "-main.db";
    static final public String WORK_DB_NAME = APP_NAME.toLowerCase() + "-work.db";
    static final public String ROOT_PAGE = ProjectsComponent.ROOT;
    static final public String DEFAULT_WD = "." + File.separator + "work" + File.separator + "${NAME}";
}
