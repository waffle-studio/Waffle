package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Log;
import spark.Spark;

import java.util.*;

public class LogsComponent extends AbstractAccessControlledComponent {
  private final int logBundleSize = 50;
  private Mode mode;

  public LogsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public LogsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(), new LogsComponent());
    Spark.get(getUrl(Mode.GetOld), new LogsComponent(Mode.GetOld));

    HostComponent.register();
  }

  public static String getUrl(Mode mode) {
    return "/logs/" + mode.name();
  }

  public static String getUrl() {
    return "/logs";
  }

  @Override
  public void controller() {
    if (mode.equals(Mode.GetOld)) {
      int from = Integer.valueOf(request.queryParamOrDefault("from", "0"));
      renderOldLog(from);
    } else {
      renderList();
    }
  }

  private void renderOldLog(int from) {
    String body = "";

    for (Log log : Log.getList(from, logBundleSize)) {
      body += Html.element("tr", null,
        Html.element("td", null, log.getLevel().name() ),
        Html.element("td", null, log.getTimestamp() ),
        Html.element("td", null, log.getMessage() )
      );
    }

    response.body(body);
  }

  private void renderList() {
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
        final int[] lastRowId = {0};
        Lte.Table table;
        return
          Lte.card(null, null,
          Lte.table("table-condensed table-sm", table = new Lte.Table() {
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
              for (Log log : Log.getList(-1, logBundleSize)) {
                list.add(new Lte.TableRow(
                  log.getLevel().name(),
                  log.getTimestamp(),
                  log.getMessage()
                  )
                );
                lastRowId[0] = log.getRowid();
              }
              return list;
            }
          }) + Html.javascript(
            "var insert_new_log = function(l, t, m) { document.getElementById('" + table.id.toString() + "').insertAdjacentHTML('afterbegin', '" +
              Html.element("tr", null,
                Html.element("td", null, "' + l + '"),
                Html.element("td", null, "' + t + '"),
                Html.element("td", null, "' + m + '")
                ).replaceAll("\n", "")
              + "'); };"
          )
          , null, null, "p-0")
            + Html.javascript("var last = " + lastRowId[0] + ";"
              + "var loadOldLog = function(){simpleget('" + getUrl(Mode.GetOld)
              + "?from=' + last,function(res){document.getElementById('"
              + table.id.toString() + "').insertAdjacentHTML('beforeend',res);});last-=" + logBundleSize
              + ";if(last <= 0){document.getElementById('oldLogButton').style.display='none';}};"
          )
            + Html.div("text-center",
            Html.a("javascript:loadOldLog()", null, new Html.Attributes(Html.value("id", "oldLogButton")),
              Html.fasIcon("plus-circle")));
      }
    }.render(this);
  }

  public enum Mode {Default, GetOld}
}
