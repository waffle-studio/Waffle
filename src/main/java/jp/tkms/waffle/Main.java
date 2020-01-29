package jp.tkms.waffle;

import jp.tkms.waffle.component.*;
import jp.tkms.waffle.component.updater.SystemUpdater;
import spark.Spark;

import static spark.Spark.*;

public class Main {
  public static boolean hibernateFlag = false;

  public static void main(String[] args) {
    System.out.println("PID: " + java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);

    Runtime.getRuntime().addShutdownHook(new Thread()
    {
      @Override
      public void run()
      {
        if (!hibernateFlag) {
          hibernate();
        }
        return;
      }
    });

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");

    staticFiles.location("/static");

    ErrorComponent.register();

    redirect.get("/", Constants.ROOT_PAGE);

    after(((request, response) -> {
      PollingThread.startup();
    }));

    BrowserMessageComponent.register();

    ProjectsComponent.register();
    JobsComponent.register();
    HostsComponent.register();

    SystemComponent.register();
    SigninComponent.register();

    PollingThread.startup();

    new Thread(){
      @Override
      public void run() {
        while (!hibernateFlag) {
          try {
            currentThread().sleep(60000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          System.gc();
        }
        this.stop();
        return;
      }
    }.start();

    new SystemUpdater(null);

    return;
  }

  public static void hibernate() {
    System.out.println("System will hibernate");
    hibernateFlag = true;
    PollingThread.waitForShutdown();
    System.out.println("System hibernated");
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Spark.stop();
    System.exit(0);
    return;
  }


}
