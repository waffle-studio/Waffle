package jp.tkms.aist;

public class Config {
    public static final int POLLING_TIME = 15000;
    public static final int SHORT_POLLING_TIME = 1500;

    public static final int MAX_CAT_RECHECK = 10;
    public static final int MAX_RERUN = 10;
    public static final int DEFUALT_MAX_SSH_CHANNEL = 10;

    public static final String GROUP_ID = "gaa50073";
    public static final String DEFAULT_WALLTIME = "1:00:00";

    public static final String LOCAL_WORKBASE_DIR = "work";
    public static final String REMOTE_WORKBASE_DIR = "/groups1/gaa50073/moji/work";
    public static final String REMOTE_WORKDIR = "/groups1/gaa50073/moji/tmp";

    public static final String UPLOAD_URL = "http://localhost:8001/";

    public static final String DATA_FILE = "waffle.dat";

    public static final int CONTROL_PORT = 8002;
    public static final boolean ENABLE_QUICKMODE = true;
}
