package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Host;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class JobsComponent extends AbstractComponent {
  private Mode mode;

  ;
  private String requestedId;
  public JobsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public JobsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(), new JobsComponent());
    Spark.get(getUrl("add"), new JobsComponent(Mode.Add));
    Spark.post(getUrl("add"), new JobsComponent(Mode.Add));

    HostComponent.register();
  }

  public static String getUrl() {
    return "/jobs";
  }

  public static String getUrl(String id) {
    return "/jobs/" + id;
  }

  @Override
  public void controller() {
    if (mode == Mode.Add) {
    } else {
     renderJobList();
    }
  }

  private void renderJobList() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Jobs";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          "Jobs"));
      }

      @Override
      protected String pageContent() {
        return Lte.card(null, null,
          Lte.table("table-condensed", getHostTableHeader(), getHostTableRow())
          , null, null, "p-0");
      }
    }.render(this);
  }

  private ArrayList<Lte.TableHeader> getHostTableHeader() {
    ArrayList<Lte.TableHeader> list = new ArrayList<>();
    list.add(new Lte.TableHeader("width:8em;", "ID"));
    list.add(new Lte.TableHeader("", "Name"));
    return list;
  }

  private ArrayList<Lte.TableRow> getHostTableRow() {
    ArrayList<Lte.TableRow> list = new ArrayList<>();
    for (Host host : Host.getList()) {
      list.add(new Lte.TableRow(
        Html.a(HostComponent.getUrl(host), null, null,  host.getShortId()),
         host.getName())
      );
    }
    return list;
  }

  public enum Mode {Default, Add}
}
