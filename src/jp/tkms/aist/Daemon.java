package jp.tkms.aist;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

public class Daemon extends Thread {
    private boolean isAlive = false;
    private CommonComponent commonComponent;
    private ArrayList<String> commandArray;
    private HashMap<String, String> varMap;
    private ArrayList<String> resultArray;
    private SocketChannel channel;
    private Work currentWork;
    private boolean isQuickMode;

    public Daemon(CommonComponent commonComponent) {
        this.commonComponent = commonComponent;
        commandArray = new ArrayList<>();
        varMap = new HashMap<>();
        resultArray = new ArrayList<>();
        channel = null;
        currentWork = null;
        isQuickMode = Config.ENABLE_QUICKMODE;
    }

    public boolean eval() {
        boolean result = true;
        String loadedScript = "";

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
                switch (command.toUpperCase()) {
                    //WORK
                    case "SETUP":
                        currentWork.setup();
                        break;
                    case "SAVE":
                        currentWork.save();
                        break;
                    case "UNLOAD":
                        commonComponent.unloadWork(currentWork);
                        currentWork = null;
                        break;
                    case "SCRIPT":
                        loadedScript += (currentWork.getTextFile(args.get(0)) + "\n");
                        break;
                    case "NEWSET":
                        UUID expSetId = currentWork.newExpSet(args.get(1), args.get(2), args.get(3), args.get(4)).getUuid();
                        defineVar(args.get(0), expSetId.toString());
                        break;
                    case "ADDEXP": {
                        ExpSet expSet = currentWork.getExpSet(args.get(0));
                        args.remove(0);
                        String val = "";
                        for (String str : args) {
                            val += str + ' ';
                        }
                        val = val.substring(0, val.length() - 1);
                        expSet.addExp(new Exp(val));
                        break;
                    }
                    case "RUNEXP": {
                        ExpSet expSet = currentWork.getExpSet(args.get(0));
                        expSet.run(commonComponent.getPollingMonitor());
                        break;
                    }
                    case "LIST": {
                        switch (args.get(0).toUpperCase()) {
                            case "SET":
                                for (ExpSet expSet : currentWork.getExpSetMap().values()) {
                                    addResult(expSet.toString());
                                }
                                break;
                        }
                        break;
                    }


                    //COMMON
                    case "ECHO":
                        String val = "";
                        for (String str : args) {
                            val += str + ' ';
                        }
                        val = val.substring(0, val.length()-1);
                        addResult(val);
                        break;
                    case "CHWORK":
                        currentWork = commonComponent.getWork(args.get(0));
                        break;
                    case "MKWORK":
                        currentWork = new Work(args.get(0));
                        currentWork.setup();
                        commonComponent.addWork(currentWork);
                        break;
                    case "DEFINE":
                        defineVar(args.get(0), args.get(1));
                        break;
                    case "ADD":
                        defineVar(args.get(0), Pattern.compile("\\.0$").matcher(String.valueOf(Double.valueOf(args.get(1)) + Double.valueOf(args.get(2)))).replaceFirst(""));
                        break;
                    case "HIBERNATE":
                        try {
                            commonComponent.save(Config.DATA_FILE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        shutdown();
                        break;
                    case "IF":
                        switch (args.get(1)) {
                            case "eq":
                                if (args.get(0).equals(args.get(2))) {
                                    i = gotoLabel(args.get(3));
                                }
                                break;
                            case "ne":
                                if (!args.get(0).equals(args.get(2))) {
                                    i = gotoLabel(args.get(3));
                                }
                                break;
                            case "==":
                                if (Double.valueOf(args.get(0)).doubleValue() == Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(args.get(3));
                                }
                                break;
                            case "!=":
                                if (Double.valueOf(args.get(0)).doubleValue() != Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(args.get(3));
                                }
                                break;
                            case ">=":
                                if (Double.valueOf(args.get(0)).doubleValue() >= Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(args.get(3));
                                }
                                break;
                            case "<=":
                                if (Double.valueOf(args.get(0)).doubleValue() <= Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(args.get(3));
                                }
                                break;
                            case ">":
                                if (Double.valueOf(args.get(0)).doubleValue() > Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(args.get(3));
                                }
                                break;
                            case "<":
                                if (Double.valueOf(args.get(0)).doubleValue() < Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(args.get(3));
                                }
                                break;
                        }
                        break;
                    case "LABEL":
                        break;
                    case "SHUTDOWN":
                        shutdown();
                        break;
                    default:
                        addResult("#INVALID COMMAND: " + commandArray.get(i));
                }
            } catch (IndexOutOfBoundsException e) {
                addResult("#INVALID COMMAND(IOoB): " + commandArray.get(i));
            } catch (NullPointerException e) {
                addResult("#INVALID COMMAND(NP): " + commandArray.get(i));
            }
        }

        commandArray.clear();

        input(loadedScript);

        return result;
    }

    private void addResult(String result) {
        System.out.println(result);
        resultArray.add(result);
    }

    private int gotoLabel(String label) {
        int i = 0;
        for (; i < commandArray.size(); i++) {
            if (commandArray.get(i).toUpperCase().equals("LABEL " + label.toUpperCase())) {
                return i;
            }
        }
        return i;
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

    public String getCurrentWorkName() {
        if (currentWork == null) {
            return "(null)";
        }
        return currentWork.getName();
    }

    public void input(String commands) {
        Scanner scanner = new Scanner(commands);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();
            if (command.toUpperCase().equals("EVAL")) {
                eval();
                break;
            } else if (command.toUpperCase().equals("EXIT")) {
                try {
                    channel.close();
                    commandArray.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            } else if (command.equals("")) {
                if (isQuickMode) {
                    eval();
                    break;
                } else {
                    continue;
                }
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
            //if (isQuickMode) { input("EVAL"); }
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
        /*
        while (isAlive) {
            input();
            try { Thread.sleep(Config.SHORT_POLLING_TIME); } catch (InterruptedException e) { e.printStackTrace(); }
        }
        */
        try (ServerSocketChannel listener = ServerSocketChannel.open();) {
            listener.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
            listener.bind(new InetSocketAddress(Config.CONTROL_PORT));
            System.out.println("[Server listening on port " + Config.CONTROL_PORT + "...]");
            while (isAlive) {
                if (channel == null || !channel.isConnected()) {
                    channel = listener.accept();
                    System.out.printf("[ACCEPT %s]%n", channel);
                }
                channel.write(Charset.forName("UTF-8").encode(CharBuffer.wrap(getCurrentWorkName() + "> ")));
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                channel.read(buffer);
                buffer.flip();
                String in = Charset.forName("UTF-8").decode(buffer).toString();
                input(in);
                System.out.println(getCurrentWorkName() + "> " + in + "");
                for (String result : resultArray) {
                    channel.write(Charset.forName("UTF-8").encode(CharBuffer.wrap(result + "\n")));
                }
                resultArray.clear();
                //Bytes.copy(channel, channel);
                //System.out.printf("CLOSE %s%n", channel);
            }
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        commonComponent.getPollingMonitor().shutdown();
    }
}
