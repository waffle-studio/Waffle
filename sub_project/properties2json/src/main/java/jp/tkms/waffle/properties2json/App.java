package jp.tkms.waffle.properties2json;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class App {

    public static void main(String[] args) {
      if (args.length < 1) {
        System.exit(1);
      }

      Properties properties = new Properties();
      try {
        properties.load(new FileInputStream(args[0]));
      } catch (IOException e) {
        e.printStackTrace();
      }

      JSONObject jsonObject = new JSONObject();

      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        String value = entry.getValue().toString();
        try {
          double num = Double.parseDouble(value);
          if (num == (int)num) {
            jsonObject.put(entry.getKey().toString(), Integer.valueOf(value));
          } else {
            jsonObject.put(entry.getKey().toString(), num);
          }
        } catch (NumberFormatException e) {
          if (value.toLowerCase().equals("true") || value.toLowerCase().equals("false")) {
            jsonObject.put(entry.getKey().toString(), Boolean.valueOf(value));
          } else {
            jsonObject.put(entry.getKey().toString(), value);
          }
        }
      }

      System.out.println(jsonObject.toString(2));
    }
}
