package jp.tkms.aist;

import com.jcraft.jsch.JSchException;

import java.util.ArrayList;

public class ExpSet {
    String seriesName;
    String preScript;
    String postScript;
    String script;
    ArrayList<Exp> expList;
    ArrayList<ExpPack> expPackList;

    public ExpSet(String seriesName, String preScript, String postScript, String script, ArrayList<Exp> expList) {
        this.seriesName = seriesName;
        this.preScript = preScript;
        this.postScript = postScript;
        this.script = script;
        this.expList = expList;
    }

    public ExpSet(String seriesName, String preScript, String postScript, String script) {
        this(seriesName, preScript, postScript, script, new ArrayList<>());
    }

    public void addExp(Exp exp) {
        exp.setExpSet(this);
        expList.add(exp);
    }

    private ArrayList<ExpPack> makeExpPacks() {
        ArrayList<ExpPack> expPackList = new ArrayList<>();

        ArrayList<Exp> nonFinishedExpList = new ArrayList<>();
        for (Exp exp : expList) {
            if (exp.getStatus() != Exp.Status.FINISHED && exp.getStatus() != Exp.Status.SUBMITTED) {
                nonFinishedExpList.add(exp);
            }
        }

        for (int i = 0; i < nonFinishedExpList.size();) {
            int packSize = AbciResourceSelector.getPackSize(nonFinishedExpList.size() - i);
            ExpPack expPack = new ExpPack();
            for (int n = 0; n < packSize && i < nonFinishedExpList.size(); n++) {
                expPack.addExp(nonFinishedExpList.get(i++));
            }
            expPackList.add(expPack);
        }

        return expPackList;
    }

    public void run(PollingMonitor monitor) throws JSchException {
        for (int c = 0; c <= Config.MAX_RERUN; c++) {
            expPackList = makeExpPacks();

            for (ExpPack expPack : expPackList) {
                expPack.run(monitor);
            }

            while (true) {
                int finished = 0;
                for (ExpPack expPack : expPackList) {
                    if (expPack.getStatus() == ExpPack.Status.FINISHED) {
                        finished++;
                    }
                }
                if (finished >= expPackList.size()) {
                    break;
                }

                try {
                    Thread.sleep(Config.SHORT_POLLING_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            int finishedCount = 0;
            for (Exp exp : expList) {
                if (exp.getStatus() == Exp.Status.FINISHED) {
                    finishedCount++;
                }
            }
            if (finishedCount == expList.size()) {
                break;
            }
        }
    }
}
