package jp.tkms.waffle;

import jp.tkms.waffle.component.*;
import jp.tkms.waffle.component.updater.SystemUpdater;
import jp.tkms.waffle.data.util.ResourceFile;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.stream.IntStream;

import static spark.Spark.*;

public class Main {
  public static final int PID = Integer.valueOf(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
  public static boolean hibernateFlag = false;
  public static boolean restartFlag = false;

  public static void main(String[] args) {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");

    System.out.println("PID: " + PID);
    int port = 4567;

    if (args.length >= 1) {
      if (Integer.valueOf(args[0]) >= 1024) {
        port = Integer.valueOf(args[0]);
      }
    } else {
      port = IntStream.range(port, 65535)
        .filter(i -> {
          try (ServerSocket socket = new ServerSocket(i, 1, InetAddress.getByName("localhost"))) {
            return true;
          } catch (IOException e) {
            return false;
          }
        })
        .findFirst().orElseThrow(IllegalStateException::new); // Finding free port from 4567
    }
    port(port);
    System.out.println("PORT: " + port);

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

    staticFiles.location("/static");

    ErrorComponent.register();

    redirect.get("/", Constants.ROOT_PAGE);

    after(((request, response) -> {
      PollingThread.startup();
    }));

    BrowserMessageComponent.register();

    ProjectsComponent.register();
    TemplatesComponent.register();
    JobsComponent.register();
    HostsComponent.register();
    ConductorTemplatesComponent.register();
    ListenerTemplatesComponent.register();

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
        return;
      }
    }.start();

    (new ScriptingContainer(LocalContextScope.THREADSAFE)).terminate();

    new SystemUpdater(null);

    return;
  }

  public static void hibernate() {
    new Thread(){
      @Override
      public void run() {
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
        if (restartFlag) {
          restartProcess();
        } else {
          System.exit(0);
        }
        return;
      }
    }.start();
    return;
  }

  public static void restart() {
    restartFlag = true;
    hibernate();
  }

  public static void restartProcess()
  {
    try {
      final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
      final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());

      if(!currentJar.getName().endsWith(".jar"))
        return;

      final ArrayList<String> command = new ArrayList<String>();
      command.add(javaBin);
      command.add("-jar");
      command.add(currentJar.getPath());

      final ProcessBuilder builder = new ProcessBuilder(command);
      System.out.println(builder.toString());
      builder.start();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.exit(0);
  }

}
