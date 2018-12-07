package jp.tkms.aist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Daemon extends Thread {
    private boolean isAlive = false;
    private PollingMonitor pollingMonitor;

    public Daemon(PollingMonitor pollingMonitor) {
        this.pollingMonitor = pollingMonitor;
    }

    public boolean command(String command) {
        String[] commandArray = command.split(" ");
        return false;
    }

    public boolean input() {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        try {
            command = bufferedReader.readLine();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return command(command);
    }

    public void shutdown() {
        isAlive = false;
    }

    @Override
    public synchronized void start() {
        isAlive = true;
        super.start();
    }

    @Override
    public void run() {
        while (isAlive) {
            try { Thread.sleep(Config.SHORT_POLLING_TIME); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}
