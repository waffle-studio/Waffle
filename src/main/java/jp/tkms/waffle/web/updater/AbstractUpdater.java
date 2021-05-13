package jp.tkms.waffle.web.updater;

import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.data.web.BrowserMessage;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class AbstractUpdater {
  private static ArrayList<AbstractUpdater> updaterList = new ArrayList<AbstractUpdater>(Arrays.asList(
    new GeneralUpdater(),
    new RunStatusUpdater(),
    new SystemUpdater(),
    new LogUpdater()
  ));

  abstract public String templateBody();
  abstract public String scriptArguments();
  abstract public String scriptBody();

  public static String getUpdaterElements() {
    String template = "";
    String script = "";
    for (AbstractUpdater updater : updaterList) {
      template += updater.templateBody();
      script += "var " + updater.getClass().getSimpleName() + " = function("
        + updater.scriptArguments() + ") {" + updater.scriptBody() + "};";
    }
    return Html.element("div",new Html.Attributes(Html.value("style", "display:none;")), template) + Html.javascript(script);
  }

  public AbstractUpdater() {
    // this method is used in initialization.
  }

  public AbstractUpdater(String... values) {
    BrowserMessage.addMessage(createUpdateScript(this.getClass(), values));
  }

  static String listByComma(String... values) {
    String result = "";
    for (int i = 0; i < values.length; i++) {
      String value = values[i];
      if (value != null) {
        result += (result != "" ? ',' : "") + value;
      }
    }
    return result;
  }

  public static String createUpdateScript(Class clazz, String... values) {
    return clazz.getSimpleName() + "(" + listByComma(values) + ");";
  }
}
