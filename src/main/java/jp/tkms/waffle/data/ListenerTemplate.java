package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.util.ResourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ListenerTemplate extends DirectoryBaseData {
  protected static final String KEY_LISTENER_TEMPLATE = "listener_template";
  private static final String KEY_LISTENER = "listener";
  //private static final String KEY_ARGUMENTS = "arguments";
  //private static final String KEY_FUNCTIONS = "functions";

  private String arguments = null;

  public ListenerTemplate(String name) {
    super(ListenerTemplate.class, getBaseDirectoryPath().resolve(name));
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

    initializeWorkDirectory();

    try {
      Files.list(getBaseDirectoryPath()).forEach(path -> {
        if (Files.isDirectory(path)) {
          listenerTemplates.add(new ListenerTemplate(path.getFileName().toString()));
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }

    return listenerTemplates;
  }

  public static ListenerTemplate create(String name) {
    createDirectories(getBaseDirectoryPath().resolve(name));
    return new ListenerTemplate(name);
  }

  public static Path getBaseDirectoryPath() {
    return PropertyFileData.getWaffleDirectoryPath().resolve(Constants.LISTENER_TEMPLATE);
  }

  @Override
  protected Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_LISTENER_TEMPLATE + Constants.EXT_JSON);
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath().resolve(name);
  }

  public String getScriptFileName() {
    return "main.rb";
  }

  public Path getScriptPath() {
    return getDirectoryPath().resolve(getScriptFileName());
  }

  public String getScript() {
    return getFileContents(getScriptPath());
  }

  public void updateScript(String script) {
    updateFileContents(getScriptPath(), script);
  }
}
