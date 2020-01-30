package jp.tkms.waffle.data.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceFile {
  private static ResourceFile staticInstance = new ResourceFile();

  public static String getContents(String path) {
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
      e.printStackTrace();
    }
    return contents.toString();
  }

}
