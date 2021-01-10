package jp.tkms.waffle.web.component.misc;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.MainTemplate;
import jp.tkms.waffle.data.util.RubyScript;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

import static jp.tkms.waffle.web.template.Html.*;

public class SystemComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  public enum Mode {Default, Hibernate, Restart, Update, DebugReport, ReduceRubyContainerCache}

  public SystemComponent(Mode mode) {
    this.mode = mode;
  }

  static public void register() {
    Spark.get(getUrl(), new SystemComponent(Mode.Default));
    Spark.get(getUrl(Mode.Hibernate), new SystemComponent(Mode.Hibernate));
    Spark.get(getUrl(Mode.Restart), new SystemComponent(Mode.Restart));
    Spark.get(getUrl(Mode.Update), new SystemComponent(Mode.Update));
    Spark.get(getUrl(Mode.DebugReport), new SystemComponent(Mode.DebugReport));
    Spark.get(getUrl(Mode.ReduceRubyContainerCache), new SystemComponent(Mode.ReduceRubyContainerCache));
  }

  public static String getUrl() {
    return "/system";
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
        renderSystem();
    }
  }

  void redirectToReferer() {
    String referer = request.headers("Referer");
    if (referer == null || "".equals(referer)) {
      referer = "/";
    }
    response.redirect(referer);
  }

  void renderSystem() {
    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "System";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList("System"));
      }

      @Override
      protected String pageContent() {
        return
          Lte.card(fasIcon("terminal") + "Properties", null,
            div(null,
              Lte.readonlyTextInputWithCopyButton("PID", String.valueOf(Main.PID)),
              Lte.readonlyTextInputWithCopyButton("Version", Main.VERSION),
              Lte.readonlyTextInputWithCopyButton("Working Directory", Constants.WORK_DIR.toString())
            )
            ,null, "card-success", null
          ) +
          Lte.card(fasIcon("wrench") + "Control", null,
          div(null,
            a(SystemComponent.getUrl(SystemComponent.Mode.Hibernate),
              Lte.button("warning",
                fasIcon("power-off", "nav-icon") + "Hibernate")
            ),
            hr(),
            div("text-right",
              a(SystemComponent.getUrl(Mode.Update),
                Lte.button("secondary",
                  fasIcon("cloud-download-alt", "nav-icon") + "System Update")
              )
            )
          )
          ,null, "card-primary", null
        ) +
        Lte.card(
          "LICENSE : " + element("strong", null, "Copyright &copy; 2019 Waffle Developer Team"),
           null,
          element("pre", null, ResourceFile.getContents("/LICENSE.md"))
          ,null, "card-secondary", null
        );
      }
    }.render(this);
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
        return Html.div(null, Html.div(null, RubyScript.debugReport(), " ", a(SystemComponent.getUrl(Mode.ReduceRubyContainerCache), "ReduceCache")) );
      }
    }.render(this);
  }
}
