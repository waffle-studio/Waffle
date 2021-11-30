package jp.tkms.waffle.data.util;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.nio.file.Path;

public class JsonWriter {
  final static ObjectWriter writer = new ObjectMapper().writer(new DefaultPrettyPrinter());

  public static void writeValue(Path path, WrappedJson jsonObject) throws IOException {
    synchronized (PathLocker.getLocker(path)) {
      writer.writeValue(path.toFile(), jsonObject);
    }
  }

  public static void writeValue(Path path, WrappedJsonArray jsonArray) throws IOException {
    synchronized (PathLocker.getLocker(path)) {
      writer.writeValue(path.toFile(), jsonArray);
    }
  }
}
