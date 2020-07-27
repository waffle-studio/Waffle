package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.ValueType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Registry extends ProjectData implements Map<Object, Object>, DataDirectory {
  protected static final String KEY_REGISTRY = "registry";
  private static final String KEY_VALUE = "value";

  public Registry(Project project) {
    super(project);
  }

  public static ArrayList<KeyValue> getList(Project project) {
    ArrayList<KeyValue> keyValueList = new ArrayList<>();

    for (File file : getBaseDirectoryPath(project).toFile().listFiles()) {
      if (file.isFile()) {
        try {
          keyValueList.add(new KeyValue(file.getName(), Files.readString(file.toPath())));
        } catch (IOException e) {
          ErrorLogMessage.issue(e);
        }
      }
    }

    return keyValueList;
  }

  public ArrayList<KeyValue> getList() {
    return getList(getProject());
  }

  static Object get(Project project, String key, ValueType type, Object defaultValue) {
    Object result = null;

    Path path = getBaseDirectoryPath(project).resolve(key);
    if (Files.exists(path)) {
      synchronized (project) {
        try {
          if (ValueType.Integer.equals(type)) {
            result = Integer.valueOf(Files.readString(path));
          } else if (ValueType.Double.equals(type)) {
            result = Double.valueOf(Files.readString(path));
          } else if (ValueType.Boolean.equals(type)) {
            result = Boolean.valueOf(Files.readString(path));
          } else if (ValueType.String.equals(type)) {
            result = Files.readString(path);
          } else {
            result = Files.readAllBytes(path);
          }
        } catch (IOException e) {
        }
      }
    }

    return (result == null ? defaultValue : result);
  }

  public Object get(String key) {
    return get(getProject(), key, null, null);
  }

  public Object get(String key, Object defaultValue) {
    return get(getProject(), key, null, defaultValue);
  }

  static void set(Project project, String key, Object value) {
    if (value != null) {
      synchronized (project) {
        Path path = getBaseDirectoryPath(project).resolve(key);
        if (!Files.exists(path)) {
          try {
            Files.createDirectories(path.getParent());
            path.toFile().createNewFile();
          } catch (IOException e) {
            ErrorLogMessage.issue(e);
          }
        }
        try {
          FileWriter filewriter = new FileWriter(path.toFile());
          filewriter.write(value.toString());
          filewriter.close();
        } catch (IOException e) {
          ErrorLogMessage.issue(e);
        }
      }
    }
  }

  public void set(String key, Object value) {
    set(getProject(), key, value);
  }

  public static String getString(Project project, String key, String defaultValue) {
    return (String)get(project, key, ValueType.String, defaultValue);
  }

  public String getString(String key, String defaultValue) {
    return getString(getProject(), key, defaultValue);
  }

  public static Integer getInteger(Project project, String key, Integer defaultValue) {
    return (Integer) get(project, key, ValueType.Integer, defaultValue);
  }

  public Integer getInteger(String key, Integer defaultValue) {
    return getInteger(getProject(), key, defaultValue);
  }

  public static Double getDouble(Project project, String key, Double defaultValue) {
    return (Double) get(project, key, ValueType.Double, defaultValue);
  }

  public Double getDouble(String key, Double defaultValue) {
    return getDouble(getProject(), key, defaultValue);
  }

  public static Boolean getBoolean(Project project, String key, Boolean defaultValue) {
    return (Boolean) get(project, key, ValueType.Boolean, defaultValue);
  }

  public Boolean getBoolean(String key, Boolean defaultValue) {
    return getBoolean(getProject(), key, defaultValue);
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean containsKey(Object o) {
    return false;
  }

  @Override
  public boolean containsValue(Object o) {
    return false;
  }

  @Override
  public Object get(Object o) {
    return get(o.toString());
  }

  @Override
  public Object put(Object o, Object o2) {
    set(o.toString(), o2);
    return o2;
  }

  @Override
  public Object remove(Object o) {
    return null;
  }

  @Override
  public void putAll(Map<?, ?> map) {

  }

  @Override
  public void clear() {

  }

  @Override
  public Set<Object> keySet() {
    return null;
  }

  @Override
  public Collection<Object> values() {
    return null;
  }

  @Override
  public Set<Entry<Object, Object>> entrySet() {
    return null;
  }

  public Object obj() {
    return new Object();
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath(getProject());
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(Constants.DOT_INTERNAL).resolve(KEY_REGISTRY);
  }

  public static class KeyValue extends AbstractMap.SimpleEntry<String, String> {
    public KeyValue(String key, String value) {
      super(key, value);
    }
  }
}
