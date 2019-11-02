package jp.tkms.waffle;

import jp.tkms.waffle.component.*;
import jp.tkms.waffle.data.Browser;

import static spark.Spark.redirect;
import static spark.Spark.staticFiles;

public class Main {
  public static void main(String[] args) {

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");

    staticFiles.location("/static");

    ErrorComponent.register();

    redirect.get("/", Environment.ROOT_PAGE);

    BrowserMessageComponent.register();

    ProjectsComponent.register();
    JobsComponent.register();
    HostsComponent.register();

    TestComponent.register();

    Browser.updateDB();
    PollingThread.startup();
  }
}
