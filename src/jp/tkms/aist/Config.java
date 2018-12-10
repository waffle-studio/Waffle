package jp.tkms.aist;

public class Config {
    public static final int POLLING_TIME = 15000;
    public static final int SHORT_POLLING_TIME = 1500;

    public static final int MAX_CAT_RECHECK = 10;
    public static final int MAX_RERUN = 10;
    public static final int MAX_SSH_CHANNEL = 5;

    public static final String GROUP_ID = "gaa50073";
    public static final String WALLTIME = "1:00:00";

    public static final String WORKBASE_DIR = "~/tmp/";

    public static final String UPLOAD_URL = "http://localhost:8001/";

    public static final String DATA_FILE = "waffle.dat";

    public static final int CONTROL_PORT = 8002;
}
