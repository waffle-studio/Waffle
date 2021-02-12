package jp.tkms.waffle;

import jp.tkms.waffle.data.job.ExecutableRunTaskStore;
import jp.tkms.waffle.data.job.SystemTaskStore;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.util.InstanceCache;
import jp.tkms.waffle.data.util.PathLocker;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.script.ruby.util.RubyScript;
import jp.tkms.waffle.web.component.job.JobsComponent;
import jp.tkms.waffle.web.component.log.LogsComponent;
import jp.tkms.waffle.web.component.misc.BrowserMessageComponent;
import jp.tkms.waffle.web.component.misc.ErrorComponent;
import jp.tkms.waffle.web.component.misc.SigninComponent;
import jp.tkms.waffle.web.component.misc.SystemComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.template.ConductorTemplatesComponent;
import jp.tkms.waffle.web.component.template.ListenerTemplatesComponent;
import jp.tkms.waffle.web.component.template.TemplatesComponent;
import jp.tkms.waffle.web.component.computer.ComputersComponent;
import spark.Spark;

import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static spark.Spark.*;

public class Main {
  public static final int PID = Integer.valueOf(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
  public static final String VERSION = getVersionId();
  public static int port = 4567;
  public static boolean aliveFlag = true;
  public static boolean hibernateFlag = false;
  public static boolean restartFlag = false;
  public static boolean updateFlag = false;
  public static ExecutorService interfaceThreadPool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, new LinkedBlockingQueue());
  public static ExecutorService systemThreadPool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, new LinkedBlockingQueue());
  public static ExecutorService filesThreadPool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, new LinkedBlockingQueue());
  public static SystemTaskStore systemTaskStore = null;
  public static ExecutableRunTaskStore jobStore = null;
  private static WatchService fileWatchService = null;
  private static HashMap<Path, Runnable> fileChangedEventListenerMap = new HashMap<>();
  private static Thread fileWatcherThread;
  private static Thread pollingThreadWalkerThread;
  private static Thread gcInvokerThread;
  private static Thread commandLineThread;
  public static SimpleDateFormat DATE_FORMAT_FOR_WAFFLE_ID = new SimpleDateFormat("yyyyMMddHHmmss");
  public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static void main(String[] args) {
    //NOTE: for https://bugs.openjdk.java.net/browse/JDK-8246714
    URLConnection.setDefaultUseCaches("classloader", false);
    URLConnection.setDefaultUseCaches("jar", false);

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");
    DATE_FORMAT_FOR_WAFFLE_ID.setTimeZone(TimeZone.getTimeZone("GMT+9"));
    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+9"));

    //Check already running process
    try {
      if (Constants.PID_FILE.toFile().exists()) {
        if (Runtime.getRuntime().exec("kill -0 " + new String(Files.readAllBytes(Constants.PID_FILE))).waitFor() == 0) {
          System.err.println("The WAFFLE on '" + Constants.WORK_DIR + "' is already running.");
          System.err.println("You should hibernate it if you want startup WAFFLE on this console.");
          System.err.println("(If you want to force startup, delete '" + Constants.PID_FILE.toString() + "'.)");
          aliveFlag = false;
          System.exit(1);
        }
      }
      Files.createDirectories(Constants.PID_FILE.getParent());
      Files.write(Constants.PID_FILE, (String.valueOf(PID)).getBytes());
      Constants.PID_FILE.toFile().deleteOnExit();
    } catch (IOException | InterruptedException e) {
      ErrorLogMessage.issue(e);
    }

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

    Runtime.getRuntime().addShutdownHook(new Thread()
    {
      @Override
      public void run()
      {
        if (!hibernateFlag) {
          hibernate();
        }

        while (aliveFlag) {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            // not needed to output
          }
        }
        return;
      }
    });

    InfoLogMessage.issue("Version is " + VERSION);
    InfoLogMessage.issue("PID is " + PID);
    InfoLogMessage.issue("Web port is " + port);

    try {
      fileWatchService = FileSystems.getDefault().newWatchService();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    fileWatcherThread = new Thread("Waffle_FileWatcher"){
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

    try {
      systemTaskStore = SystemTaskStore.load();
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }

    try {
      jobStore = ExecutableRunTaskStore.load();
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }

    staticFiles.location("/static");

    ErrorComponent.register();

    redirect.get("/", Constants.ROOT_PAGE);

    BrowserMessageComponent.register();

    ProjectsComponent.register();
    TemplatesComponent.register();
    JobsComponent.register();
    ComputersComponent.register();
    ConductorTemplatesComponent.register();
    ListenerTemplatesComponent.register();
    LogsComponent.register();

    SystemComponent.register();
    SigninComponent.register();

    pollingThreadWalkerThread =  new Thread("Waffle_Polling") {
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
    pollingThreadWalkerThread.start();

    gcInvokerThread = new Thread("Waffle_GCInvoker"){
      @Override
      public void run() {
        while (!hibernateFlag) {
          try {
            currentThread().sleep(60000);
          } catch (InterruptedException e) {
            return;
          }
          InstanceCache.gc();
        }
        return;
      }
    };
    gcInvokerThread.start();

    commandLineThread = new Thread("Waffle_CommandLine"){
      @Override
      public void run() {
        try {
          Scanner in = new Scanner(System.in);
          while (true) {
            String command = in.nextLine();
            System.out.println("-> " + command);
            switch (command) {
              case "exit":
              case "quit":
              case "hibernate":
                hibernate();
                break;
              case "kill":
                aliveFlag = false;
                System.exit(1);
                break;
            }
          }
        } catch (Exception e) {
          InfoLogMessage.issue("console command feature is disabled (could not gets user inputs)");
          return;
        }
      }
    };
    commandLineThread.start();

    RubyScript.process(scriptingContainer -> {
      scriptingContainer.runScriptlet("print \"\"");
    });

    return;
  }

  public static Thread hibernate() {
    Thread processThread = null;

    if (hibernateFlag) {
      return processThread;
    }

    processThread = new Thread(){
      @Override
      public void run() {
        System.out.println("(0/6) System will hibernate");
        hibernateFlag = true;

        try {
          commandLineThread.interrupt();
          fileWatcherThread.interrupt();
          pollingThreadWalkerThread.interrupt();
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

        Spark.stop();
        Spark.awaitStop();
        System.out.println("(4/6) Web interface stopped");
        try {
          interfaceThreadPool.shutdown();
          interfaceThreadPool.awaitTermination(7, TimeUnit.DAYS);
        } catch (Throwable e) {}
        System.out.println("(5/6) Web interface common threads stopped");

        PathLocker.waitAllCachedFiles();
        System.out.println("(6/6) File buffer threads stopped");

        if (restartFlag) {
          restartProcess();
        }

        System.out.println("System hibernated");

        aliveFlag = false;
        System.exit(0);
        return;
      }
    };

    processThread.start();

    return processThread;
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
    String version = ResourceFile.getContents("/version.txt").trim();
    if ("".equals(version)) {
      return "?";
    }
    return version;
  }
}
