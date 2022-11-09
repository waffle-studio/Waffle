package jp.tkms.waffle;

import jp.tkms.utils.debug.DebugString;
import jp.tkms.utils.value.Init;
import jp.tkms.waffle.communicator.JobNumberLimitedLocalSubmitter;
import jp.tkms.waffle.data.computer.MasterPassword;
import jp.tkms.waffle.data.internal.InternalFiles;
import jp.tkms.waffle.data.log.Log;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.util.InstanceCache;
import jp.tkms.waffle.data.util.PathLocker;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.data.web.Password;
import jp.tkms.waffle.data.web.UserSession;
import jp.tkms.waffle.inspector.InspectorMaster;
import jp.tkms.waffle.manager.ManagerMaster;
import jp.tkms.waffle.script.ruby.util.RubyScript;
import jp.tkms.waffle.web.component.job.JobsComponent;
import jp.tkms.waffle.web.component.log.LogsComponent;
import jp.tkms.waffle.web.component.misc.*;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.computer.ComputersComponent;
import jp.tkms.waffle.web.component.websocket.PushNotifier;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.impl.SimpleLoggerConfiguration;
import spark.Spark;
import spark.embeddedserver.EmbeddedServers;
import spark.embeddedserver.jetty.EmbeddedJettyFactory;
import spark.embeddedserver.jetty.JettyServerFactory;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {
  public static final int PID = Integer.valueOf(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
  public static final String VERSION = new Init<String>().call(() -> {
    String version = ResourceFile.getContents("/version.txt").trim();
    if ("".equals(version)) {
      return "?";
    }
    return version;
  });

  public static int port = 8400;
  public static boolean aliveFlag = true;
  public static boolean hibernatingFlag = false;
  public static boolean restartFlag = false;
  public static boolean updateFlag = false;
  public static ExecutorService interfaceThreadPool = Executors.newWorkStealingPool();//new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, new LinkedBlockingQueue());
  public static ExecutorService systemThreadPool = Executors.newWorkStealingPool();//new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, new LinkedBlockingQueue());
  private static final FileWatcherThread fileWatcherThread = new FileWatcherThread();
  private static final Thread gcInvokerThread = new GCInvokerThread();
  private static final Thread commandLineThread = new CommandLineThread();
  public static final SimpleDateFormat DATE_FORMAT_FOR_WAFFLE_ID = new Init<SimpleDateFormat>().call(new SimpleDateFormat("yyyyMMddHHmmss"), (o) -> {
    o.setTimeZone(TimeZone.getTimeZone("GMT+9"));
  } );
  public static final SimpleDateFormat DATE_FORMAT = new Init<SimpleDateFormat>().call(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"), (o) -> {
    o.setTimeZone(TimeZone.getTimeZone("GMT+9"));
  });

  public static void main(String[] args) {
    //NOTE: for https://bugs.openjdk.java.net/browse/JDK-8246714
    URLConnection.setDefaultUseCaches("classloader", false);
    URLConnection.setDefaultUseCaches("jar", false);

    //NOTE: for including slf4j to jar file
    SimpleLoggerConfiguration simpleLoggerConfiguration = new SimpleLoggerConfiguration();
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");

    avoidMultipleLaunch();

    if (args.length >= 1 && Integer.valueOf(args[0]) >= 1024) {
      port = Integer.valueOf(args[0]);
    } else {
      port = getValidPort();
    }

    addShutdownHook();

    resetAutomaticRestartingFlag();

    InfoLogMessage.issue("Version is " + VERSION);
    InfoLogMessage.issue("PID is " + PID);
    InfoLogMessage.issue("Web port is " + port);

    MasterPassword.registerWithAuthenticate(System.getenv("MASTER_PASS"));
    if (MasterPassword.isRegistered()) {
      UserSession.loadCache();
    }

    fileWatcherThread.start();

    initSpark(port);
    gcInvokerThread.start();
    commandLineThread.start();
    bootRubyScript();

    ManagerMaster.startup();
    InspectorMaster.startup();

    tryToOpenWebBrowser();

    return;
  }

  private static void avoidMultipleLaunch() {
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
  }

  private static int getValidPort() {
    return IntStream.range(port, 65535)
      .filter(i -> {
        try (ServerSocket socket = new ServerSocket(i, 1, InetAddress.getByName("localhost"))) {
          return true;
        } catch (IOException e) {
          return false;
        }
      })
      .findFirst().orElseThrow(IllegalStateException::new); // Finding free port from 8400
  }

  private static void addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread()
    {
      @Override
      public void run()
      {
        if (!hibernatingFlag) {
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
  }

  private static void initSpark(int port) {
    EmbeddedServers.add(EmbeddedServers.Identifiers.JETTY, new EmbeddedJettyFactory(new JettyServerFactory() {
      @Override
      public Server create(int maxThreads, int minThreads, int threadTimeoutMillis) {
        return create(null);
      }

      @Override
      public Server create(ThreadPool threadPool) {
        Server server = new Server();
        server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", Math.pow(1024, 3));
        return server;
      }
    }));

    Spark.port(port);

    Spark.staticFiles.location("/static");
    ErrorComponent.register();
    PushNotifier.register();
    BrowserMessageComponent.register();
    ProjectsComponent.register();
    JobsComponent.register();
    ComputersComponent.register();
    LogsComponent.register();
    SystemComponent.register();
    SigninComponent.register();
    //HelpComponent.register();
    Spark.redirect.get("/", Constants.ROOT_PAGE);

    Spark.init();
  }

  private static void tryToOpenWebBrowser() {
    if (System.getenv().containsKey(Constants.WAFFLE_OPEN_COMMAND)) {
      try {
        Runtime.getRuntime().exec(System.getenv().get(Constants.WAFFLE_OPEN_COMMAND) + " http://localhost:" + port + "/");
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }

  private static void bootRubyScript() {
    RubyScript.process(scriptingContainer -> {
      scriptingContainer.runScriptlet("print \"\"");
    });
  }

  static class FileWatcherThread extends Thread {
    private WatchService fileWatchService = null;
    private HashMap<Path, Runnable> fileChangedEventListenerMap = new HashMap<>();

    public FileWatcherThread() {
      super("Waffle_FileWatcher");

      try {
        fileWatchService = FileSystems.getDefault().newWatchService();
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    @Override
    public void run() {
      try {
        WatchKey watchKey = null;
        while (!hibernatingFlag && (watchKey = fileWatchService.take()) != null) {
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

    public void registerFileChangeEventListener(Path path, Runnable function) {
      fileChangedEventListenerMap.put(path, function);
      try {
        path.register(fileWatchService, StandardWatchEventKinds.ENTRY_MODIFY);
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }

  static class GCInvokerThread extends Thread {
    public GCInvokerThread() {
      super("Waffle_GCInvoker");
    }

    @Override
    public void run() {
      while (!hibernatingFlag) {
        try {
          currentThread().sleep(60000);
        } catch (InterruptedException e) {
          return;
        }
        InstanceCache.gc();
      }
      return;
    }
  }

  static class CommandLineThread extends Thread {

    public CommandLineThread() {
      super("Waffle_CommandLine");
    }
    @Override
    public void run() {
      try {
        Scanner in = new Scanner(System.in);
        while (true) {
          String[] commands = in.nextLine().split(" ");
          System.out.println("-> " + DebugString.toString(commands));
          switch (commands[0]) {
            case "exit":
            case "quit":
            case "hibernate":
              hibernate();
              break;
            case "RESET":
              reset();
              break;
            case "KILL":
              aliveFlag = false;
              System.exit(1);
              break;
            case "pass":
              if (Password.authenticate(commands[1])) {
                MasterPassword.register(commands[1]);
              }
              break;
          }
        }
      } catch (Exception e) {
        InfoLogMessage.issue("console command feature is disabled (could not gets user inputs)");
        return;
      }
    }
  }

  public static Thread hibernate() {
    Thread processThread = null;

    if (hibernatingFlag) {
      return processThread;
    }

    processThread = new Thread(){
      @Override
      public void run() {
        System.out.println("(0/6) System will hibernate");
        hibernatingFlag = true;

        try {
          UserSession.saveCache();
          commandLineThread.interrupt();
          fileWatcherThread.interrupt();
          gcInvokerThread.interrupt();
        } catch (Throwable e) {}
        System.out.println("(1/7) Misc. components stopped");

        InspectorMaster.waitForShutdown();
        System.out.println("(2/7) Inspector stopped");

        try {
          systemThreadPool.shutdown();
          systemThreadPool.awaitTermination(7, TimeUnit.DAYS);
        } catch (Throwable e) {}
        System.out.println("(3/7) System common threads stopped");

        Spark.stop();
        Spark.awaitStop();
        System.out.println("(4/7) Web interface stopped");
        try {
          interfaceThreadPool.shutdown();
          interfaceThreadPool.awaitTermination(7, TimeUnit.DAYS);
        } catch (Throwable e) {}
        System.out.println("(5/7) Web interface common threads stopped");

        PathLocker.waitAllCachedFiles();
        System.out.println("(6/7) File buffer threads stopped");

        Log.close();
        System.out.println("(7/7) Logger threads stopped");

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
    fileWatcherThread.registerFileChangeEventListener(path, function);
  }

  public static void restart() {
    restartFlag = true;
    hibernate();
  }

  public static void reset() {
    aliveFlag = false;
    resetProcess();
    restartProcess();
    System.exit(1);
  }

  public static void update() {
    updateFlag = true;
    restart();
  }

  public static void restartProcess() {
    try {
      /*
      final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
      final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if(!currentJar.getName().endsWith(".jar")) {
        return;
      }
       */

      if (updateFlag) {
        System.out.println("System updating...");
        updateProcess();
      }

      /*
      System.out.println("If it is not restart automatically, please restart " + currentJar.toString());
      final ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", currentJar.getPath(), String.valueOf(port));
      System.out.println("System will fork and restart");
      builder.start();
       */
      createAutomaticRestartingFlag();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  public static void resetProcess() {
      System.out.println("REMOVING RUNNING STATUS...");
    try {
      try (Stream<Path> stream = Files.list(InternalFiles.getPath())) {
        stream.filter(path -> Files.isDirectory(path)).filter(path -> !path.endsWith("waffle-jre")).forEach(path -> {
          try {
          JobNumberLimitedLocalSubmitter.deleteDirectory(path.toString());
          } catch (Exception e) {
            //nop
          }
        });
      }
    } catch (Exception e) {
      //nop
    }
  }

  private static void createAutomaticRestartingFlag() throws IOException {
    Files.createDirectories(Constants.AUTO_START_FILE.getParent());
    Files.createFile(Constants.AUTO_START_FILE);
  }

  private static void resetAutomaticRestartingFlag() {
    try {
      Files.deleteIfExists(Constants.AUTO_START_FILE);
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
  }

  public static void updateProcess() {
    try {
      final File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if(!currentJar.getName().endsWith(".jar")) {
        throw new Exception("Failed to detect JAR file name");
      }

      final URL url = new URL(Constants.JAR_URL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setAllowUserInteraction(false);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestMethod("GET");
      connection.connect();
      if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
        throw new Exception("Failed to download JAR file");
      }

      DataInputStream dataInStream = new DataInputStream(connection.getInputStream());
      DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream( currentJar.getPath() )));
      byte[] buffer = new byte[4096];
      int readByte = 0;
      while(-1 != (readByte = dataInStream.read(buffer))){
        dataOutStream.write(buffer, 0, readByte);
      }
      dataInStream.close();
      dataOutStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
