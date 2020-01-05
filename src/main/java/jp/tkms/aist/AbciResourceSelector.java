package jp.tkms.aist;

public class AbciResourceSelector {
    public static String getResourceText(int size) {
        if (size <= 5) {
            return "rt_C.small=1";
        } else if (size <= 20) {
            return "rt_C.large=1";
        } else {
            return "rt_F=1";
        }
    }

    public static int getPackSize(int nonPackedSize) {
        if (nonPackedSize >= 40) {
            return 40;
        } else if (nonPackedSize >= 15) {
            return 20;
        } else {
            return 5;
        }
    }
}
