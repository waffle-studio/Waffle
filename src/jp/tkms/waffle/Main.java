package jp.tkms.waffle;

import jp.tkms.waffle.component.*;
import jp.tkms.waffle.data.Browser;
import spark.Spark;

import static spark.Spark.*;

public class Main {
  public static boolean hibernateFlag = false;

  public static void main(String[] args) {

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");

    staticFiles.location("/static");

    ErrorComponent.register();

    redirect.get("/", Environment.ROOT_PAGE);

    after(((request, response) -> {
      PollingThread.startup();
    }));

    BrowserMessageComponent.register();

    ProjectsComponent.register();
    JobsComponent.register();
    HostsComponent.register();

    SystemComponent.register();

    Browser.updateDB();
    PollingThread.startup();

    return;
  }

  public static void hibernate() {
    hibernateFlag = true;
    Spark.stop();
  }
}
