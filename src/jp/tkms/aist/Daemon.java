package jp.tkms.aist;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.*;

public class Daemon extends Thread {
    private boolean isAlive = false;
    private CommonComponent commonComponent;
    private ArrayList<String> commandArray;
    private HashMap<String, String> varMap;

    public Daemon(CommonComponent commonComponent) {
        this.commonComponent = commonComponent;
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
                    case "SLEEP":
                        try { Thread.sleep(Integer.valueOf(args.get(0))); } catch (InterruptedException e) { e.printStackTrace(); }
                        break;
                    case "DEFINE":
                        defineVar(args.get(0), args.get(1));
                        break;
                    case "ADD":
                        defineVar(args.get(0), String.valueOf(Double.valueOf(args.get(1)) + Double.valueOf(args.get(2))));
                        break;
                    case "SAVE":
                        try {
                            if (args.size() >= 1) {
                                commonComponent.save(args.get(0));
                            } else {
                                commonComponent.save(Config.DATA_FILE);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
                        System.out.println("INVALID COMMAND: " + commandArray.get(i));
                }
            } catch (IndexOutOfBoundsException e) {
                System.out.println("INVALID COMMAND(IOoB): " + commandArray.get(i));
            }
        }

        commandArray.clear();
        return result;
    }

    private int gotoLabel(String label) {
        int i = 0;
        for (; i < commandArray.size(); i++) {
            if (commandArray.get(i).equals("LABEL " + label)) {
                return i;
            }
        }
        return i;
    }

    private String defineVar(String name, String value) {
        if (name.length() > 0 && name.charAt(0) == '$') {
            varMap.put(name, value);
            return value;
        }
        return "";
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
            } else if (command.equals("")) {
                continue;
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
        /*
        while (isAlive) {
            input();
            try { Thread.sleep(Config.SHORT_POLLING_TIME); } catch (InterruptedException e) { e.printStackTrace(); }
        }
        */
        try (ServerSocketChannel listener = ServerSocketChannel.open();) {
            listener.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
            listener.bind(new InetSocketAddress(Config.CONTROL_PORT));
            System.out.println("Server listening on port " + Config.CONTROL_PORT + "...");
            while (isAlive) {
                try (SocketChannel channel = listener.accept();) {
                    System.out.printf("ACCEPT %s%n", channel);
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    channel.read(buffer);
                    buffer.flip();
                    String in = Charset.forName("UTF-8").decode(buffer).toString();
                    input(in);
                    System.out.println(in);
                    //Bytes.copy(channel, channel);
                    //System.out.printf("CLOSE %s%n", channel);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
