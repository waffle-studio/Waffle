package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.Log;
import spark.Spark;

import java.util.*;

public class LogsComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  private String requestedId;
  public LogsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public LogsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(), new LogsComponent());

    HostComponent.register();
  }

  public static String getUrl() {
    return "/logs";
  }

  @Override
  public void controller() {
    renderJobList();
  }

  private void renderJobList() {
    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "Logs";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          "Logs"));
      }

      @Override
      protected String pageContent() {
        return
          Lte.card(null, null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("width:1em;", "Level"));
              list.add(new Lte.TableValue("width:10em;", "Timestamp"));
              list.add(new Lte.TableValue("", "Message"));
              return list;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (Log log : Log.getList(-1, 50)) {
                list.add(new Lte.TableRow(
                  log.getLevel().name(),
                  log.getTimestamp(),
                  log.getMessage()
                  )
                );
              }
              Collections.reverse(list);
              return list;
            }
          }) + Html.javascript(
            "var debug = function() {}; var info = debug; var warn = debug; var error = debug;"
          )
          , null, null, "p-0");
      }
    }.render(this);
  }

  public enum Mode {Default}
}
