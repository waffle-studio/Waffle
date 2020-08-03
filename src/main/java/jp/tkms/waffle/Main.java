package jp.tkms.waffle;

import jp.tkms.waffle.component.*;
import jp.tkms.waffle.component.updater.SystemUpdater;
import jp.tkms.waffle.data.JobStore;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.data.util.RubyScript;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import spark.Spark;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static spark.Spark.*;

public class Main {
  public static final int PID = Integer.valueOf(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
  public static final String VERSION = getVersionId();
  public static int port = 4567;
  public static boolean hibernateFlag = false;
  public static boolean restartFlag = false;
  public static boolean updateFlag = false;
  public static ExecutorService interfaceThreadPool = Executors.newCachedThreadPool();
  public static ExecutorService systemThreadPool = Executors.newCachedThreadPool();
  public static JobStore jobStore = null;
  private static WatchService fileWatchService = null;
  private static HashMap<Path, Runnable> fileChangedEventListenerMap = new HashMap<>();
  private static Thread fileWatcherThread;
  private static Thread pollingThreadWakerThread;
  private static Thread gcInvokerThread;

  public static void main(String[] args) {
    //NOTE: for https://bugs.openjdk.java.net/browse/JDK-8246714
    URLConnection.setDefaultUseCaches("classloader", false);
    URLConnection.setDefaultUseCaches("jar", false);

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");

    System.out.println("PID: " + PID);

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

    try {
      jobStore = JobStore.load();
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }

    try {
      fileWatchService = FileSystems.getDefault().newWatchService();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    fileWatcherThread = new Thread(){
      @Override
      public void run() {
        try {
          WatchKey watchKey = null;
          while (!hibernateFlag && (watchKey = fileWatchService.take()) != null) {
            for (WatchEvent<?> event : watchKey.pollEvents()) {
              Runnable runnable = fileChangedEventListenerMap.get((Path)watchKey.watchable());
              if (runnable != null) {
                runnable.run();
              }
            }
            watchKey.reset();
          }
        } catch (InterruptedException e) {
          return;
        }
      }
    };
    fileWatcherThread.start();

    staticFiles.location("/static");

    ErrorComponent.register();

    redirect.get("/", Constants.ROOT_PAGE);

    BrowserMessageComponent.register();

    ProjectsComponent.register();
    TemplatesComponent.register();
    JobsComponent.register();
    HostsComponent.register();
    ConductorTemplatesComponent.register();
    ListenerTemplatesComponent.register();
    LogsComponent.register();

    SystemComponent.register();
    SigninComponent.register();

    pollingThreadWakerThread =  new Thread() {
      @Override
      public void run() {
        while (!hibernateFlag) {
          PollingThread.startup();
          try {
            currentThread().sleep(5000);
          } catch (InterruptedException e) {
            return;
          }
        }
        return;
      }
    };
    pollingThreadWakerThread.start();

    gcInvokerThread = new Thread(){
      @Override
      public void run() {
        while (!hibernateFlag) {
          try {
            currentThread().sleep(60000);
          } catch (InterruptedException e) {
            return;
          }
          System.gc();
        }
        return;
      }
    };
    gcInvokerThread.start();

    RubyScript.process(scriptingContainer -> {
      scriptingContainer.runScriptlet("print \"\"");
    });

    //new SystemUpdater(null);

    return;
  }

  public static void hibernate() {
    if (hibernateFlag) {
      return;
    }

    new Thread(){
      @Override
      public void run() {
        System.out.println("(0/6) System will hibernate");
        hibernateFlag = true;

        try {
          fileWatcherThread.interrupt();
          pollingThreadWakerThread.interrupt();
          gcInvokerThread.interrupt();
        } catch (Throwable e) {}
        System.out.println("(1/6) Misc. components stopped");

        PollingThread.waitForShutdown();
        System.out.println("(2/6) Polling system stopped");

        try {
          systemThreadPool.shutdown();
          systemThreadPool.awaitTermination(7, TimeUnit.DAYS);
        } catch (Throwable e) {}
        System.out.println("(3/6) System common threads stopped");

        try {
          jobStore.save();
        } catch (IOException e) {
          ErrorLogMessage.issue(e);
        }
        System.out.println("(4/6) Job store stopped");

        Spark.stop();
        Spark.awaitStop();
        System.out.println("(5/6) Web interface stopped");
        try {
          interfaceThreadPool.shutdown();
          interfaceThreadPool.awaitTermination(7, TimeUnit.DAYS);
        } catch (Throwable e) {}
        System.out.println("(6/6) Web interface common threads stopped");

        if (restartFlag) {
          restartProcess();
        }

        System.out.println("System hibernated");
        System.exit(0);
        return;
      }
    }.start();

    return;
  }

  public static void registerFileChangeEventListener(Path path, Runnable function) {
    fileChangedEventListenerMap.put(path, function);
    try {
      path.register(fileWatchService, StandardWatchEventKinds.ENTRY_MODIFY);
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
  }

  public static void restart() {
    restartFlag = true;
    hibernate();
  }

  public static void update() {
    updateFlag = true;
    restart();
  }

  public static void restartProcess() {
    try {
      final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
      final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());

      if(!currentJar.getName().endsWith(".jar")) {
        return;
      }

      final ArrayList<String> command = new ArrayList<String>();
      command.add(javaBin);
      command.add("-jar");
      command.add(currentJar.getPath());
      command.add(String.valueOf(port));

      if (updateFlag) {
        updateProcess();
      }

      final ProcessBuilder builder = new ProcessBuilder(command);
      System.out.println("System will fork and restert");
      builder.start();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void updateProcess() {
    try {
      final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());

      if(!currentJar.getName().endsWith(".jar")) {
        return;
      }

      URL url = new URL(Constants.JAR_URL);

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setAllowUserInteraction(false);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestMethod("GET");
      connection.connect();

      int httpStatusCode = connection.getResponseCode();

      if(httpStatusCode != HttpURLConnection.HTTP_OK){
        throw new Exception();
      }

      DataInputStream dataInStream = new DataInputStream( connection.getInputStream());

      DataOutputStream dataOutStream = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(
        currentJar.getPath()
      )));

      byte[] b = new byte[4096];
      int readByte = 0;

      while(-1 != (readByte = dataInStream.read(b))){
        dataOutStream.write(b, 0, readByte);
      }

      dataInStream.close();
      dataOutStream.close();

      Main.restart();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static String getVersionId() {
    String version = ResourceFile.getContents("/version.txt");
    if ("".equals(version)) {
      return "?";
    }
    return version;
  }
}
