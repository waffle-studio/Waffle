package jp.tkms.aist;

import java.util.ArrayList;

public class ExpSet {
    String preScript;
    String postScript;
    String script;
    ArrayList<String> experimentList;

    public ExpSet(String preScript, String postScript, String script, ArrayList<String> experimentList) {
        this.preScript = preScript;
        this.postScript = postScript;
        this.script = script;
        this.experimentList = experimentList;
    }

    public ExpPack makeExpPack(int size) {
        ExpPack expPack = null;

        return expPack;
    }
}
