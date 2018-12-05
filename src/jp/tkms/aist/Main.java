package jp.tkms.aist;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {

    final static String keyStore = "/home/takami/.ssh/abci";

    public static void main(String[] args) throws JSchException, IOException {
        JSch jsch = new JSch();
        jsch.addIdentity(keyStore + "/id_rsa");
        Session sessionAs = jsch.getSession("aaa10259dp", "as.abci.ai", 22);
        sessionAs.setConfig("StrictHostKeyChecking", "no");
        sessionAs.connect();
        int assingedPort = sessionAs.setPortForwardingL(0, "es", 22);

        Session sessionEs = jsch.getSession("aaa10259dp", "localhost", assingedPort);
        sessionEs.setConfig("StrictHostKeyChecking", "no");
        sessionEs.connect();

        ChannelExec channel = (ChannelExec)sessionEs.openChannel("exec");

        channel.setCommand("echo");
        channel.connect();

        // エラーメッセージ用Stream
        BufferedInputStream errStream = new BufferedInputStream(channel.getErrStream());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (true) {
            int len = errStream.read(buf);
            if (len <= 0) {
                break;
            }
            outputStream.write(buf, 0, len);
        }
        // エラーメッセージ取得する
        String message = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

        channel.disconnect();
        int sleepCount = 0;
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        } while (!channel.isClosed() && sleepCount++ < 100);

        // コマンドの戻り値を取得する
        int returnCode = channel.getExitStatus();
        System.out.println("Result: " + returnCode);

        //--------------------------------------------------

        channel = (ChannelExec)sessionEs.openChannel("exec");
        channel.setCommand("date >> /home/aaa10259dp/tmp/out");
        channel.connect();
        channel.disconnect();
        sleepCount = 0;
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        } while (!channel.isClosed() && sleepCount++ < 100);
        System.out.println("Result: " + channel.getExitStatus());

        channel = (ChannelExec)sessionEs.openChannel("exec");
        channel.setCommand("cd tmp");
        channel.connect();
        channel.disconnect();
        sleepCount = 0;
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        } while (!channel.isClosed() && sleepCount++ < 100);
        System.out.println("Result: " + channel.getExitStatus());

        channel = (ChannelExec)sessionEs.openChannel("exec");
        channel.setCommand("cd tmp && pwd >> /home/aaa10259dp/tmp/out");
        channel.connect();
        channel.disconnect();
        sleepCount = 0;
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        } while (!channel.isClosed() && sleepCount++ < 100);
        System.out.println("Result: " + channel.getExitStatus());


        channel = (ChannelExec)sessionEs.openChannel("exec");
        channel.setCommand("mkdir a");
        channel.connect();
        channel.disconnect();
        sleepCount = 0;
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        } while (!channel.isClosed() && sleepCount++ < 100);
        System.out.println("Result: " + channel.getExitStatus());

        channel = (ChannelExec)sessionEs.openChannel("exec");
        channel.setCommand("echo #!/bin/bash > batch.sh");
        channel.connect();
        channel.disconnect();
        sleepCount = 0;
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        } while (!channel.isClosed() && sleepCount++ < 100);
        System.out.println("Result: " + channel.getExitStatus());


        channel = (ChannelExec)sessionEs.openChannel("exec");
        channel.setCommand("cd ..");
        channel.connect();
        channel.disconnect();
        sleepCount = 0;
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        } while (!channel.isClosed() && sleepCount++ < 100);
        System.out.println("Result: " + channel.getExitStatus());

        //channel.connect();
        //channel.setCommand("rm -rf a");
        //System.out.println("Result: " + channel.getExitStatus());
        //channel.disconnect();

        //--------------------------------------------------

        sessionEs.disconnect();
        sessionAs.disconnect();
    }
}
