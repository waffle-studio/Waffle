package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.io.IOException;
import java.util.Date;

public class Main {

    public static void main(String[] args) throws JSchException, IOException, InterruptedException {
        Date startDate = new Date();

        PollingMonitor monitor = new PollingMonitor();
        monitor.start();

        ExpSet expSet = new ExpSet("test10","", "",
                "/home/aaa10259dp/gaa50073/moji/exe/crtest_moji_happy_cmaes_map24.sh");

        for (int test1 = 0; test1 < 5; test1++) {
            expSet.addExp(new Exp("HAPPY10000_MAP24 0 " + test1));
        }

        expSet.run(monitor);

        monitor.shutdown();

        Date endDate = new Date();
        System.out.println(startDate.toString() + " / " + endDate.toString());

        return;
    }
}
