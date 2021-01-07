package jp.tkms.waffle.web.component;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.MainTemplate;
import jp.tkms.waffle.data.util.RubyScript;
import spark.Spark;

import java.util.ArrayList;
import java.util.Map;

public class SystemComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  public enum Mode {Default, Hibernate, Restart, Update, DebugReport, ReduceRubyContainerCache}

  public SystemComponent(Mode mode) {
    this.mode = mode;
  }

  static public void register() {
    Spark.get(getUrl(Mode.Hibernate), new SystemComponent(Mode.Hibernate));
    Spark.get(getUrl(Mode.Restart), new SystemComponent(Mode.Restart));
    Spark.get(getUrl(Mode.Update), new SystemComponent(Mode.Update));
    Spark.get(getUrl(Mode.DebugReport), new SystemComponent(Mode.DebugReport));
    Spark.get(getUrl(Mode.ReduceRubyContainerCache), new SystemComponent(Mode.ReduceRubyContainerCache));
  }

  public static String getUrl(Mode mode) {
    return "/system/" + mode.name();
  }

  @Override
  public void controller() {
    logger.info(response.status() + ": " + request.url());

    switch (mode) {
      case Hibernate:
        redirectToReferer();
        Main.hibernate();
        break;
      case Restart:
        redirectToReferer();
        Main.restart();
        break;
      case Update:
        redirectToReferer();
        Main.update();
      break;
      case ReduceRubyContainerCache:
        redirectToReferer();
        RubyScript.reduceContainerCache();
        break;
      case DebugReport:
        renderDebugReport();
        break;
      default:
    }
  }

  void redirectToReferer() {
    String referer = request.headers("Referer");
    if (referer == null || "".equals(referer)) {
      referer = "/";
    }
    response.redirect(referer);
  }

  void renderDebugReport() {
    new MainTemplate(){
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "Debug Report";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<>();
      }

      @Override
      protected String pageContent() {
        return Html.div(null, Html.div(null, RubyScript.debugReport(), " ", Html.a(SystemComponent.getUrl(Mode.ReduceRubyContainerCache), "ReduceCache")) );
      }
    }.render(this);
  }
}
