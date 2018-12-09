package jp.tkms.aist;

import java.util.*;

public class Daemon extends Thread {
    private boolean isAlive = false;
    private PollingMonitor pollingMonitor;
    private ArrayList<String> commandArray;
    private HashMap<String, String> varMap;

    public Daemon(PollingMonitor pollingMonitor) {
        this.pollingMonitor = pollingMonitor;
        commandArray = new ArrayList<>();
        varMap = new HashMap<>();
    }

    public boolean eval() {
        boolean result = true;

        for (int i = 0; i < commandArray.size(); i++) {
            ArrayList<String> line = new ArrayList<>(Arrays.asList(commandArray.get(i).split("\\s")));
            if (line.size() <= 0) { continue; }
            String command = "";
            ArrayList<String> args = new ArrayList<>();
            for (String str : line) {
                if (command.equals("")) {
                    command = appearVar(str);
                } else {
                    args.add(appearVar(str));
                }
            }

            try {
                switch (command) {
                    case "ECHO":
                        for (String str : args) {
                            System.out.print(str + ' ');
                        }
                        System.out.println();
                        break;
                    case "DEFINE":
                        defineVar(args.get(0), args.get(1));
                        break;
                    case "SHUTDOWN":
                        shutdown();
                        break;
                    default:
                        System.out.println("INVALID COMMAND: " + commandArray.get(i));
                }
            } catch (IndexOutOfBoundsException e) {
                System.out.println("INVALID COMMAND(IOoB): " + commandArray.get(i));
            }
        }

        commandArray.clear();
        return result;
    }

    private String defineVar(String name, String value) {
        varMap.put('$' + name, value);
        return value;
    }

    private String appearVar(String org) {
        if (org.length() > 0 && org.charAt(0) == '$') {
            return varMap.get(org);
        }
        return org;
    }

    public void input(String commands) {
        Scanner scanner = new Scanner(commands);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();
            if (command.equals("EVAL")) {
                eval();
                break;
            }
            this.commandArray.add(command);
        }
    }

    public void input() {
        try {
            Scanner scanner = new Scanner(System.in);
            String command = scanner.nextLine();
            input(command);
        } catch (NoSuchElementException e) {
        }
    }

    public boolean willShutdown() {
        return !isAlive;
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
            input();
            try { Thread.sleep(Config.SHORT_POLLING_TIME); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}
