package jp.tkms.waffle.sub.communicator;

import java.io.IOException;

public class Main {
    public static Thread shutdownTimer = null;
    public static void main(String[] args) {
    boolean isLocalMode = true;
    int timeout = 0;
    int shutdownTime = 0;
    int marginTime = 0;

    if (args.length != 4) {
      System.err.println("invalid arguments");
      System.err.println("<JAVA JAR> [MODE] [TIMEOUT] [FORCE SHUTDOWN TIME] [SHUTDOWN PREPARATION MARGIN]");
      System.err.println("invalid arguments");
      System.exit(1);
    }

    if (args[0].toLowerCase().equals("mpi")) {
      isLocalMode = false;
    }

    try {
      timeout = Integer.valueOf(args[1]);
      shutdownTime = Integer.valueOf(args[2]);
      marginTime = Integer.valueOf(args[3]);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      System.exit(1);
    }

    if (shutdownTime < marginTime) {
      System.err.println("SHUTDOWN PREPARATION MARGIN must be less than FORCE SHUTDOWN TIME");
      System.exit(1);
    }

    return;
  }
}
