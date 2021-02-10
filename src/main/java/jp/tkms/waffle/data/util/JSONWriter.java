package jp.tkms.waffle.data.util;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class JSONWriter {
  final static ObjectWriter writer = new ObjectMapper().writer(new DefaultPrettyPrinter());

  public static void writeValue(Path path, JSONObject jsonObject) throws IOException {
    synchronized (PathLocker.getLocker(path)) {
      writer.writeValue(path.toFile(), jsonObject.toMap());
    }
  }

  public static void writeValue(Path path, JSONArray jsonArray) throws IOException {
    synchronized (PathLocker.getLocker(path)) {
      writer.writeValue(path.toFile(), jsonArray.toList());
    }
  }
}
