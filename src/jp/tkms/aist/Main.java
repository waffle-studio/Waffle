package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws JSchException, InterruptedException, IOException {
        //Date startDate = new Date();

        CommonComponent commonComponent = CommonComponent.load(Config.DATA_FILE);

        PollingMonitor monitor = commonComponent.getPollingMonitor();
        monitor.start();

        Daemon daemon = Daemon.getInstance(commonComponent);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (daemon.isAlive()) {
                    commonComponent.save(Config.DATA_FILE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        daemon.start();

        //Date endDate = new Date();
        //System.out.println(startDate.toString() + " / " + endDate.toString());

        return;
    }
}
