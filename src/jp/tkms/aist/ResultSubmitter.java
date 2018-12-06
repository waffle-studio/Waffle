package jp.tkms.aist;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ResultSubmitter {
    public static synchronized String post(String seriesName, String resultJson) {

        HttpURLConnection con = null;
        StringBuffer result = new StringBuffer();
        try {
            String postText = "app=upload&name=" + seriesName + "&json=" + URLEncoder.encode(resultJson, "UTF-8");

            URL url = new URL(Config.UPLOAD_URL);
            con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            con.setRequestProperty("Content-Length", String.valueOf(postText.length()));
            OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
            out.write(postText);
            out.flush();
            con.connect();

            final int status = con.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                final InputStream in = con.getInputStream();
                String encoding = con.getContentEncoding();
                if (null == encoding) {
                    encoding = "UTF-8";
                }
                final InputStreamReader inReader = new InputStreamReader(in, encoding);
                final BufferedReader bufReader = new BufferedReader(inReader);
                String line = null;
                while ((line = bufReader.readLine()) != null) {
                    result.append(line);
                }
                bufReader.close();
                inReader.close();
                in.close();
            } else {
                System.out.println(status);
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return result.toString();
    }
}
