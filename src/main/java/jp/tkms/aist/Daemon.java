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
    private static Daemon instance = null;
    private static final long serialVersionUID = 1L;
    private static final char omitPrefix = '~';

    private static final ArrayList<String> emptyStringArrayList = new ArrayList<>();
    private boolean isAlive = false;
    private CommonComponent commonComponent;
    private ArrayList<String> commandArray;
    private HashMap<String, String> varMap;
    private SocketChannel channel;
    private Work currentWork;
    private boolean isQuickMode;
    private String prevInput;

    public static Daemon getInstance(CommonComponent commonComponent) {
        if (instance == null) {
            instance = new Daemon(commonComponent);
        }
        return instance;
    }

    private Daemon(CommonComponent commonComponent) {
        this.commonComponent = commonComponent;
        commandArray = new ArrayList<>();
        varMap = new HashMap<>();
        channel = null;
        currentWork = commonComponent.getWork(commonComponent.getHibernateWork());
        isQuickMode = Config.ENABLE_QUICKMODE;
        prevInput = "";
    }

    public ArrayList<String> eval(Work currentWork, ArrayList<String> commandArray) {
        ArrayList<String> resultArray = new ArrayList<>();

        for (int i = 0; i < commandArray.size(); i++) {
            ArrayList<String> line = new ArrayList<>(Arrays.asList(commandArray.get(i).split("\\s")));
            if (line.size() <= 0) { continue; }
            String command = "";
            ArrayList<String> args = new ArrayList<>();
            for (String str : line) {
                if (command.equals("")) {
                    command = appearVar(currentWork, str);
                } else {
                    args.add(appearVar(currentWork, str));
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
                        currentWork = this.currentWork = null;
                        break;
                    case "SCRIPT":
                    case "`S": {
                        String loadedScript = currentWork.getTextFile(args.get(0));
                        ArrayList<String> result = eval(currentWork, loadedScript);
                        for (String r : result) {
                            addResult(resultArray, r);
                        }
                        break;
                    }
                    case "NEWSET":
                        UUID expSetId = currentWork.newExpSet(commonComponent,
                                args.get(1),
                                currentWork.getTextFile(args.get(2)), currentWork.getTextFile(args.get(3)),
                                args.get(4)).getUuid();
                        defineVar(currentWork, args.get(0), expSetId.toString());
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
                        expSet.start();
                        break;
                    }
                    case "WALLTIME": {
                        if (args.size() >= 1) {
                            addResult(resultArray, currentWork.setWallTime(args.get(0)));
                        } else {
                            addResult(resultArray, currentWork.getWallTime());
                        }
                        break;
                    }
                    case "LOCALEXEC":
                    case "`LE": {
                        ExecuteResult result = currentWork.execOnLocal(args.toArray(new String[args.size()]));
                        addResult(resultArray, result.getOut());
                        addResult(resultArray, "EXITCODE:" + result.getExitCode());
                        defineVar(currentWork, "?", String.valueOf(result.getExitCode()));
                        break;
                    }
                    case "REMOTEEXEC":
                    case "`RE": {
                        ExecuteResult result = currentWork.execOnRemote(args.toArray(new String[args.size()]));
                        addResult(resultArray, result.getOut());
                        addResult(resultArray, "EXITCODE:" + result.getExitCode());
                        defineVar(currentWork, "?", String.valueOf(result.getExitCode()));
                        break;
                    }


                    // COMPLEX
                    case "SHOW": {
                        switch (args.get(0).toUpperCase()) {
                            case "EXP":
                                for (ExpSet expSet : currentWork.getExpSetMap().values()) {
                                    boolean isBraked = false;
                                    for (Exp exp : expSet.expList) {
                                        if (exp.getUuid().toString().equals(args.get(1))) {
                                            addResult(resultArray, exp.toString());
                                            addResult(resultArray, "# " + exp.getResult());
                                            isBraked = true;
                                            break;
                                        }
                                    }
                                    if (isBraked) { break; }
                                }
                                break;
                            case "PACK":
                                break;
                            case "SET":
                                break;
                            case "POLLING":
                                break;
                            case "WORK":
                                break;
                            default:
                                addResult(resultArray, "#INVALID COMMAND(show): " + commandArray.get(i));

                        }
                        break;
                    }
                    case "LIST":
                    case "`L": {
                        switch (args.get(0).toUpperCase()) {
                            case "EXP":
                                for (Exp exp : currentWork.getExpSetMap().get(UUID.fromString(args.get(1))).expList) {
                                    addResult(resultArray, exp.toString());
                                }
                                addResult(resultArray, "-----");
                                addResult(resultArray, "TOTAL: " + currentWork.getExpSetMap().get(UUID.fromString(args.get(1))).expList.size());
                                break;
                            case "PACK":
                                for (ExpPack expPack : currentWork.getExpSetMap().get(UUID.fromString(args.get(1))).expPackList) {
                                    addResult(resultArray, expPack.toString());
                                }
                                addResult(resultArray, "-----");
                                addResult(resultArray, "TOTAL: " + currentWork.getExpSetMap().get(UUID.fromString(args.get(1))).expPackList.size());
                                break;
                            case "SET":
                                for (ExpSet expSet : currentWork.getExpSetMap().values()) {
                                    addResult(resultArray, expSet.toString());
                                }
                                addResult(resultArray, "-----");
                                addResult(resultArray, "TOTAL: " + currentWork.getExpSetMap().size());
                                break;
                            case "POLLING":
                                for (ExpPack expPack : commonComponent.getPollingMonitor().getExpPackList()) {
                                    addResult(resultArray, expPack.toString());
                                }
                                addResult(resultArray, "-----");
                                addResult(resultArray, "TOTAL: " + commonComponent.getPollingMonitor().getExpPackList().size());
                                break;
                            case "WORK":
                                for (Work work : commonComponent.getWorkMap().values()) {
                                    addResult(resultArray, work.toString());
                                }
                                addResult(resultArray, "-----");
                                addResult(resultArray, "TOTAL: " + commonComponent.getWorkMap().size());
                                break;
                            default:
                                addResult(resultArray, "#INVALID COMMAND(list): " + commandArray.get(i));

                        }
                        break;
                    }


                    //COMMON
                    case "ECHO": {
                        String val = "";
                        for (String str : args) {
                            val += str + ' ';
                        }
                        val = val.substring(0, val.length() - 1);
                        addResult(resultArray, val);
                        break;
                    }
                    case "CHWORK":
                    case "`CW":
                        currentWork = this.currentWork = commonComponent.getWork(args.get(0));
                        break;
                    case "MKWORK":
                    case "`MW":
                        for (Work work : commonComponent.getWorkMap().values()) {
                            if (work.getName().equals(args.get((0)))) {
                                addResult(resultArray, "#INVALID COMMAND(mkwork): " + commandArray.get(i));
                                break;
                            }
                        }
                        currentWork = this.currentWork = new Work(args.get(0));
                        currentWork.setup();
                        commonComponent.addWork(this.currentWork);
                        break;
                    case "DEFINE":
                        defineVar(currentWork, args.get(0), args.get(1));
                        break;
                    case "STRCAT": {
                        String name = args.get(0);
                        args.remove(0);
                        String val = "";
                        for (String str : args) {
                            val += str;
                        }
                        defineVar(currentWork, name, val);
                        break;
                    }
                    case "COMPUTE":
                        switch (args.get(2)) {
                            case "/":
                                defineVar(currentWork, args.get(0), Pattern.compile("\\.0$").matcher(String.valueOf(Double.valueOf(args.get(1)) / Double.valueOf(args.get(3)))).replaceFirst(""));
                                break;
                            case "*":
                                defineVar(currentWork, args.get(0), Pattern.compile("\\.0$").matcher(String.valueOf(Double.valueOf(args.get(1)) * Double.valueOf(args.get(3)))).replaceFirst(""));
                                break;
                            case "-":
                                defineVar(currentWork, args.get(0), Pattern.compile("\\.0$").matcher(String.valueOf(Double.valueOf(args.get(1)) - Double.valueOf(args.get(3)))).replaceFirst(""));
                                break;
                            case "+":
                                defineVar(currentWork, args.get(0), Pattern.compile("\\.0$").matcher(String.valueOf(Double.valueOf(args.get(1)) + Double.valueOf(args.get(3)))).replaceFirst(""));
                                break;
                            default:
                                addResult(resultArray, "#INVALID COMMAND(compute): " + commandArray.get(i));
                        }
                        break;
                    case "ADD":
                        defineVar(currentWork, args.get(0), Pattern.compile("\\.0$").matcher(String.valueOf(Double.valueOf(args.get(1)) + Double.valueOf(args.get(2)))).replaceFirst(""));
                        break;
                    case "IF":
                        switch (args.get(1)) {
                            case "eq":
                                if (args.get(0).equals(args.get(2))) {
                                    i = gotoLabel(commandArray, args.get(3));
                                }
                                break;
                            case "ne":
                                if (!args.get(0).equals(args.get(2))) {
                                    i = gotoLabel(commandArray, args.get(3));
                                }
                                break;
                            case "==":
                                if (Double.valueOf(args.get(0)).doubleValue() == Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(commandArray, args.get(3));
                                }
                                break;
                            case "!=":
                                if (Double.valueOf(args.get(0)).doubleValue() != Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(commandArray, args.get(3));
                                }
                                break;
                            case ">=":
                                if (Double.valueOf(args.get(0)).doubleValue() >= Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(commandArray, args.get(3));
                                }
                                break;
                            case "<=":
                                if (Double.valueOf(args.get(0)).doubleValue() <= Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(commandArray, args.get(3));
                                }
                                break;
                            case ">":
                                if (Double.valueOf(args.get(0)).doubleValue() > Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(commandArray, args.get(3));
                                }
                                break;
                            case "<":
                                if (Double.valueOf(args.get(0)).doubleValue() < Double.valueOf(args.get(2)).doubleValue()) {
                                    i = gotoLabel(commandArray, args.get(3));
                                }
                                break;
                            default:
                                addResult(resultArray, "#INVALID COMMAND(if): " + commandArray.get(i));
                        }
                        break;
                    case "LABEL":
                        break;
                    case "POLLING_FORCECHECK":
                        commonComponent.getPollingMonitor().forceCheck();
                        break;
                    case "MAX_SSH_CHANNEL":
                        if (args.size() >= 1) {
                            addResult(resultArray,
                                    "MAX SSH CHANNEL: " +
                                            commonComponent.setMaxSshChannel(Integer.valueOf(args.get(0))));
                        } else {
                            addResult(resultArray,
                                    "MAX SSH CHANNEL: " +
                                            commonComponent.getMaxSshChannel());
                        }
                        break;
                    case "HIBERNATE":
                    case "`H":
                        commonComponent.getPollingMonitor().shutdown();
                        while (!commonComponent.getPollingMonitor().isStoped()) {
                            try { Thread.sleep(Config.SHORT_POLLING_TIME); } catch (InterruptedException e) { e.printStackTrace(); }
                        }

                        try {
                            for (Work work: commonComponent.getWorkMap().values()) {
                                work.save();
                            }
                            commonComponent.setHibernateWork(currentWork.getName());
                            commonComponent.save(Config.DATA_FILE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        shutdown();
                        break;
                    case "SHUTDOWN":
                        shutdown();
                        break;
                    default:
                        addResult(resultArray, "#INVALID COMMAND: " + commandArray.get(i));
                }
            } catch (IndexOutOfBoundsException e) {
                addResult(resultArray, "#INVALID COMMAND(IOoB): " + commandArray.get(i));
            } catch (NullPointerException e) {
                addResult(resultArray, "#INVALID COMMAND(NP): " + commandArray.get(i));
            }
        }

        commandArray.clear();

        return resultArray;
    }

    public ArrayList<String> eval(Work currentWork, String script) {
        ArrayList<String> commandArray = new ArrayList<>();
        Scanner scanner = new Scanner(script);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();
            if (command.toUpperCase().equals("")) {
                continue;
            }
            commandArray.add(command);
        }
        return eval(currentWork, commandArray);
    }

    private void addResult(ArrayList<String> resultArray, String result) {
        System.out.println(result);
        if (resultArray.size() >= 2) {
            String prev1 = resultArray.get(resultArray.size() -1);
            String prev2 = resultArray.get(resultArray.size() -2);
            if (prev1.length() > 1 && prev2.length() > 1 && result.length() > 1) {
                if (prev1.charAt(0) == omitPrefix && prev2.charAt(0) == omitPrefix && result.charAt(0) == omitPrefix) {
                    resultArray.set(resultArray.size() -1, result);
                    return;
                }
            }
        }
        resultArray.add(result);
    }

    private int gotoLabel(ArrayList<String> commandArray,String label) {
        int i = 0;
        for (; i < commandArray.size(); i++) {
            if (commandArray.get(i).toUpperCase().equals("LABEL " + label.toUpperCase())) {
                return i;
            }
        }
        return i;
    }

    private String defineVar(Work currentWork, String name, String value) {
        varMap.put('$' + name, value);
        if (currentWork != null) {
            currentWork.getVarMap().put('$' + name, value);
        }
        return value;
    }

    private String appearVar(Work currentWork, String org) {
        if (org.length() > 0 && org.charAt(0) == '$') {
            String result = null;
            if (currentWork != null) {
                result = currentWork.getVarMap().get(org);
            }
            if (result == null) {
                result = varMap.get(org);
            }
            return result;
        }
        return org;
    }

    public String getCurrentWorkName() {
        if (currentWork == null) {
            return "(null)";
        }
        return currentWork.getName();
    }

    public ArrayList<String> input(String commands) {
        ArrayList<String> resultArray = emptyStringArrayList;
        Scanner scanner = new Scanner(commands);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();

            if (command.equals(".")) {
                command = prevInput;
            }

            if (command.toUpperCase().equals("EVAL")) {
                resultArray = eval(currentWork, commandArray);
                break;
            } else if (command.toUpperCase().equals("EXIT")) {
                try {
                    channel.close();
                    commandArray.clear();
                    prevInput = "";
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            } else if (command.equals("")) {
                if (isQuickMode) {
                    resultArray = eval(currentWork, commandArray);
                    break;
                } else {
                    continue;
                }
            }
            this.commandArray.add(command);
            prevInput = command;
        }
        return resultArray;
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
        while (isAlive) {
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
                    ArrayList<String> resultArray = input(in);
                    System.out.println(getCurrentWorkName() + "> " + in + "");
                    for (String result : resultArray) {
                        channel.write(Charset.forName("UTF-8").encode(CharBuffer.wrap(result + "\n")));
                    }
                }
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }

        commonComponent.getPollingMonitor().shutdown();
        ResultSubmitter.shutdown();

        System.out.println("Daemon terminated");
    }
}
