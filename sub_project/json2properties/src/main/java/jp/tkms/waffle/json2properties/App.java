package jp.tkms.waffle.json2properties;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class App {

    public static void main(String[] args) {
      if (args.length < 1) {
        System.exit(1);
      }

      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject = new JSONObject(new String(Files.readAllBytes(Paths.get(args[0]))));
      } catch (IOException e) {
        e.printStackTrace();
      }

      Properties properties = new Properties();

      for (Map.Entry<String, Object> entry : jsonObject.toMap().entrySet()) {
        String value = entry.getValue().toString();
        try {
          double num = Double.parseDouble(value);
          if (num == (int)num) {
            System.out.println(entry.getKey().toString() + "=" + Integer.valueOf(value));
          } else {
            System.out.println(entry.getKey().toString() + "=" + num);
          }
        } catch (NumberFormatException e) {
          if (value.toLowerCase().equals("true") || value.toLowerCase().equals("false")) {
            System.out.println(entry.getKey().toString() + "=" + value);
          } else {
            System.out.println(entry.getKey().toString() + "=" + value);
          }
        }
      }
    }
}
