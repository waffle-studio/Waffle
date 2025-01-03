package jp.tkms.waffle.data.project.workspace;

import jp.tkms.utils.concurrent.LockByKey;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.data.util.ValueType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Registry extends WorkspaceData implements Map<Object, Object>, DataDirectory {
  protected static final String KEY_REGISTRY = "registry";
  private static final String KEY_VALUE = "value";
  private static MessageDigest SHA3_512;

  static {
    try {
      SHA3_512 = MessageDigest.getInstance("SHA3-512");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  static String getDigest(String text) {
    byte[] result = SHA3_512.digest(text.getBytes());
    return String.format("%040x", new BigInteger(1, result));
  }

  public Registry(Workspace workspace) {
    super(workspace);
  }

  public static ArrayList<KeyValue> getList(Workspace workspace) {
    ArrayList<KeyValue> keyValueList = new ArrayList<>();

    for (File file : getBaseDirectoryPath(workspace).toFile().listFiles()) {
      if (file.isFile()) {
        keyValueList.add(new KeyValue(file.getName(), StringFileUtil.read(file.toPath())));
      }
    }

    return keyValueList;
  }

  public ArrayList<KeyValue> getList() {
    return getList(getWorkspace());
  }

  static Object get(Workspace workspace, String key, ValueType type, Object defaultValue) {
    Object result = null;

    Path path = getBaseDirectoryPath(workspace).resolve(getDigest(key));
    if (Files.exists(path)) {
      synchronized (workspace) {
        try {
          if (ValueType.Integer.equals(type)) {
            result = Integer.valueOf(StringFileUtil.read(path));
          } else if (ValueType.Double.equals(type)) {
            result = Double.valueOf(StringFileUtil.read(path));
          } else if (ValueType.Boolean.equals(type)) {
            result = Boolean.valueOf(StringFileUtil.read(path));
          } else if (ValueType.String.equals(type)) {
            result = StringFileUtil.read(path);
          } else {
            try (LockByKey lock = LockByKey.acquire(path.toAbsolutePath().normalize())) {
              result = Files.readAllBytes(path);
            }
          }
        } catch (IOException e) {
          ErrorLogMessage.issue(e);
        }
      }
    }

    return (result == null ? defaultValue : result);
  }

  public Object get(String key) {
    return get(getWorkspace(), key, null, null);
  }

  public Object get(String key, Object defaultValue) {
    return get(getWorkspace(), key, null, defaultValue);
  }

  static void set(Workspace workspace, String key, Object value) {
    if (value != null) {
      synchronized (workspace) {
        Path path = getBaseDirectoryPath(workspace).resolve(getDigest(key));
        if (!Files.exists(path)) {
          try {
            Files.createDirectories(path.getParent());
            path.toFile().createNewFile();
          } catch (IOException e) {
            ErrorLogMessage.issue(e);
          }
        }
        StringFileUtil.write(path, value.toString());
      }
    }
  }

  public void set(String key, Object value) {
    set(getWorkspace(), key, value);
  }

  public static String getString(Workspace workspace, String key, String defaultValue) {
    return (String)get(workspace, key, ValueType.String, defaultValue);
  }

  public String getString(String key, String defaultValue) {
    return getString(getWorkspace(), key, defaultValue);
  }

  public static Integer getInteger(Workspace workspace, String key, Integer defaultValue) {
    return (Integer) get(workspace, key, ValueType.Integer, defaultValue);
  }

  public Integer getInteger(String key, Integer defaultValue) {
    return getInteger(getWorkspace(), key, defaultValue);
  }

  public static Double getDouble(Workspace workspace, String key, Double defaultValue) {
    return (Double) get(workspace, key, ValueType.Double, defaultValue);
  }

  public Double getDouble(String key, Double defaultValue) {
    return getDouble(getWorkspace(), key, defaultValue);
  }

  public static Boolean getBoolean(Workspace workspace, String key, Boolean defaultValue) {
    return (Boolean) get(workspace, key, ValueType.Boolean, defaultValue);
  }

  public Boolean getBoolean(String key, Boolean defaultValue) {
    return getBoolean(getWorkspace(), key, defaultValue);
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
  public Path getPath() {
    return getBaseDirectoryPath(getWorkspace());
  }

  public static Path getBaseDirectoryPath(Workspace workspace) {
    return workspace.getPath().resolve(Constants.DOT_INTERNAL).resolve(KEY_REGISTRY);
  }

  public static class KeyValue extends AbstractMap.SimpleEntry<String, String> {
    public KeyValue(String key, String value) {
      super(key, value);
    }
  }
}
