package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.io.IOException;
import java.util.Date;

public class Main {

    public static void main(String[] args) throws JSchException, InterruptedException, IOException {
        Date startDate = new Date();

        CommonComponent commonComponent = CommonComponent.load(Config.DATA_FILE);

        PollingMonitor monitor = commonComponent.getPollingMonitor();
        monitor.start();

        Daemon daemon = new Daemon(commonComponent);
        daemon.start();

        if (commonComponent.getExpSetList().size() > 0) {
            ExpSet expSet = commonComponent.getExpSetList().get(0);
            for (Exp exp : expSet.expList) {
                System.out.println(exp.toString());
            }
            System.out.println(expSet.expList.size());
            //System.exit(1);
        } else {
            ExpSet expSet = new ExpSet("test11","", "",
                    "/home/aaa10259dp/gaa50073/moji/exe/crtest_moji_happy_cmaes_map24.sh");

            for (int test1 = 0; test1 < 10000; test1++) {
                expSet.addExp(new Exp("HAPPY10000_MAP24 0 " + test1));
            }

            commonComponent.addExpSet(expSet);
            commonComponent.save(Config.DATA_FILE);
            System.exit(0);

            expSet.run(monitor);
        }

        monitor.shutdown();

        Date endDate = new Date();
        System.out.println(startDate.toString() + " / " + endDate.toString());

        return;
    }
}
