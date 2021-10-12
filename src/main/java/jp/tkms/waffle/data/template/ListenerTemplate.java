package jp.tkms.waffle.data.template;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.web.Data;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.util.ResourceFile;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ListenerTemplate implements DataDirectory, PropertyFile {
  protected static final String KEY_LISTENER_TEMPLATE = "listener_template";
  private static final String KEY_LISTENER = "listener";
  //private static final String KEY_ARGUMENTS = "arguments";
  //private static final String KEY_FUNCTIONS = "functions";

  private String name;
  private String arguments = null;

  public ListenerTemplate(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static ListenerTemplate getInstance(String name) {
    ListenerTemplate listenerTemplate = null;

    if (Files.exists(getBaseDirectoryPath().resolve(name))) {
      listenerTemplate = new ListenerTemplate(name);

      if (! Files.exists(listenerTemplate.getScriptPath())) {
        listenerTemplate.createNewFile(listenerTemplate.getScriptPath());
        listenerTemplate.updateScript(ResourceFile.getContents("/ruby_actor_template.rb"));
      }
    }

    return listenerTemplate;
  }

  public static ArrayList<ListenerTemplate> getList() {
    ArrayList<ListenerTemplate> listenerTemplates = new ArrayList<>();

    Data.initializeWorkDirectory();

    for (File file : getBaseDirectoryPath().toFile().listFiles()) {
      if (file.isDirectory()) {
        listenerTemplates.add(new ListenerTemplate(file.getName()));
      }
    }

    return listenerTemplates;
  }

  public static ListenerTemplate create(String name) {
    try {
      Files.createDirectories(getBaseDirectoryPath().resolve(name));
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    return new ListenerTemplate(name);
  }

  public static Path getBaseDirectoryPath() {
    return Data.getWaffleDirectoryPath().resolve(Constants.LISTENER_TEMPLATE);
  }

  @Override
  public Path getPropertyStorePath() {
    return getPath().resolve(KEY_LISTENER_TEMPLATE + Constants.EXT_JSON);
  }

  @Override
  public Path getPath() {
    return getBaseDirectoryPath().resolve(name);
  }

  public String getScriptFileName() {
    return "main.rb";
  }

  public Path getScriptPath() {
    return getPath().resolve(getScriptFileName());
  }

  public String getScript() {
    return getFileContents(getScriptPath());
  }

  public void updateScript(String script) {
    updateFileContents(getScriptPath(), script);
  }

  JSONObject propertyStoreCache = null;
  @Override
  public JSONObject getPropertyStoreCache() {
    return propertyStoreCache;
  }
  @Override
  public void setPropertyStoreCache(JSONObject cache) {
    propertyStoreCache = cache;
  }
}
