package jp.tkms.waffle.sub.servant;

public class Constants {
  public static final String RESPONSE_SUFFIX = ".res";
  public static final String BASE = "BASE";
  public static final String STDOUT_FILE = "STDOUT.txt";
  public static final String STDERR_FILE = "STDERR.txt";
  public static final String EVENT_FILE = "EVENT.bin";
  public static final String EVENT_SEPARATOR = new String(new byte[]{0x1e});
  public static final String EVENT_VALUE_SEPARATOR = new String(new byte[]{0x1f});
  public static final String EXIT_STATUS_FILE = "EXIT_STATUS.log";
  public static final String LOCAL_SHARED = "local_shared";
  public static final int DIRECTORY_SYNCHRONIZATION_TIMEOUT = 300;
}
