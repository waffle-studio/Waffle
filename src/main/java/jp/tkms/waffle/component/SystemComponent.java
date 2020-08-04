package jp.tkms.waffle.component;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.util.RubyScript;
import spark.Spark;

import java.util.ArrayList;
import java.util.Map;

public class SystemComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  public enum Mode {Default, Hibernate, Restart, Update, DebugReport}

  public SystemComponent(Mode mode) {
    this.mode = mode;
  }

  static public void register() {
    Spark.get(getUrl(Mode.Hibernate), new SystemComponent(Mode.Hibernate));
    Spark.get(getUrl(Mode.Restart), new SystemComponent(Mode.Restart));
    Spark.get(getUrl(Mode.Update), new SystemComponent(Mode.Update));
    Spark.get(getUrl(Mode.DebugReport), new SystemComponent(Mode.DebugReport));
  }

  public static String getUrl(Mode mode) {
    return "/system/" + mode.name();
  }

  @Override
  public void controller() {
    logger.info(response.status() + ": " + request.url());

    switch (mode) {
      case Hibernate: {
        String referer = request.headers("Referer");
        if (referer == null || "".equals(referer)) {
          referer = "/";
        }
        response.redirect(referer);
        Main.hibernate();
      }
      break;
      case Restart: {
        String referer = request.headers("Referer");
        if (referer == null || "".equals(referer)) {
          referer = "/";
        }
        response.redirect(referer);
        Main.restart();
      }
      break;
      case Update: {
        String referer = request.headers("Referer");
        if (referer == null || "".equals(referer)) {
          referer = "/";
        }
        response.redirect(referer);
        Main.update();
      }
      break;
      case DebugReport:
        renderDebugReport();
        break;
      default:
    }
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
        return Html.div(null, Html.div(null, RubyScript.debugReport()) );
      }
    }.render(this);
  }
}
