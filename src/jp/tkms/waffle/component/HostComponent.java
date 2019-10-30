package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Project;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class HostComponent extends AbstractComponent {
  private Mode mode;

  ;
  private Host host;
  public HostComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public HostComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new HostComponent());
    Spark.get(getUrl(null, "edit_const_model"), new HostComponent());

    SimulatorsComponent.register();
    TrialsComponent.register();
  }

  public static String getUrl(Host host) {
    return "/host/" + (host == null ? ":id" : host.getId());
  }

  public static String getUrl(Host host, String mode) {
    return getUrl(host) + '/' + mode;
  }

  @Override
  public void controller() {
    host = Host.getInstance(request.params("id"));

    renderHost();
  }

  private void renderHost() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return host.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(HostsComponent.getUrl(), "Hosts"),
          host.getId()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        content += Lte.card(Html.faIcon("terminal") + "Basic",
          ( host.isLocal() ?  null : Html.a("", Html.faIcon("edit")) ),
          Html.div(null,
            Html.div(null,
              "Simulation Command",
              Lte.disabledTextInput(null)
            ),
            Html.div(null,
              "Version Command",
              Lte.disabledTextInput(null)
            )
          )
          , null);

        return content;
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private ArrayList<Lte.TableHeader> getProjectTableHeader() {
    ArrayList<Lte.TableHeader> list = new ArrayList<>();
    list.add(new Lte.TableHeader("width:8em;", "ID"));
    list.add(new Lte.TableHeader("", "Name"));
    return list;
  }

  private ArrayList<Lte.TableRow> getProjectTableRow() {
    ArrayList<Lte.TableRow> list = new ArrayList<>();
    for (Project project : Project.getList()) {
      list.add(new Lte.TableRow(
        Html.a("", null, null, project.getShortId()),
        project.getName())
      );
    }
    return list;
  }

  private void createProject() {

  }

  public enum Mode {Default, EditConstModel}
}
