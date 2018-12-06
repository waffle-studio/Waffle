package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws JSchException, IOException {
        PollingMonitor monitor = new PollingMonitor();
        monitor.start();

        ExpSet expSet = new ExpSet("test6","", "",
                "/home/aaa10259dp/gaa50073/moji/exe/crtest_moji_happy_cmaes_map24.sh");

        for (int test1 = 0; test1 < 10000; test1++) {
            expSet.addExp(new Exp("HAPPY10000_MAP24 0 " + test1));
        }

        expSet.run(monitor);

        monitor.shutdown();

        return;
    }
}
