package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.log.ErrorLogMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class ResourceFile {
  private static ResourceFile staticInstance = new ResourceFile();

  private static final HashMap<String, String> instanceMap = new HashMap<>();

  public static String getContents(String path) {
    String candidate = instanceMap.get(path);
    if (candidate != null) {
      return candidate;
    }

    StringBuilder contents = new StringBuilder();
    InputStream in = staticInstance.getClass().getResourceAsStream(path);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String data;
    try {
      while (true) {
        if (!((data = reader.readLine()) != null)) break;
        contents.append(data).append('\n');
      }
      reader.close();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
      return "";
    }

    instanceMap.put(path, contents.toString());
    return contents.toString();
  }

}
