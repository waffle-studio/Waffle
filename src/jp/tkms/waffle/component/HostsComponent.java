package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Project;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class HostsComponent extends AbstractComponent {
  private Mode mode;

  ;
  private String requestedId;
  public HostsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public HostsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(), new HostsComponent());
    Spark.get(getUrl("add"), new HostsComponent(Mode.Add));
    Spark.post(getUrl("add"), new HostsComponent(Mode.Add));

    HostComponent.register();
  }

  public static String getUrl() {
    return "/hosts";
  }

  public static String getUrl(String mode) {
    return "/hosts/" + mode;
  }

  @Override
  public void controller() {
    if (mode == Mode.Add) {
      if (request.requestMethod().toLowerCase().equals("post")) {
        ArrayList<Lte.FormError> errors = checkCreateProjectFormError();
        if (errors.isEmpty()) {
          addSimulator();
        } else {
          renderAddForm(errors);
        }
      } else {
        renderAddForm(new ArrayList<>());
      }
    } else {
      renderHostList();
    }
  }

  private void renderAddForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Hosts";
      }

      @Override
      protected String pageSubTitle() {
        return "Add";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(HostsComponent.getUrl(), "Hosts"),
          "Add"));
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl("add"), Html.Method.Post,
            Lte.card("New Host", null,
              Html.div(null,
                Html.inputHidden("cmd", "add"),
                Lte.formInputGroup("text", "name", null, "Name", errors),
                Html.hr(),
                Lte.formInputGroup("text", "sim_cmd", "Simulation command", "", errors),
                Lte.formInputGroup("text", "ver_cmd", "Version command", "", errors)
              ),
              Lte.formSubmitButton("success", "Add"),
              "card-warning", null
            )
          );
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private void renderHostList() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Hosts";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          "Hosts"));
      }

      @Override
      protected String pageContent() {
        return Lte.card(null,
          Html.a(getUrl("add"),
            null, null, Html.faIcon("plus-square")
          ),
          Lte.table("table-condensed", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("width:8em;", "ID"));
              list.add(new Lte.TableValue("", "Name"));
              return list;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (Host host : Host.getList()) {
                list.add(new Lte.TableRow(
                  Html.a(HostComponent.getUrl(host), null, null,  host.getShortId()),
                  host.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0");
      }
    }.render(this);
  }

  private void addSimulator() {
    Host host = Host.create(
      request.queryParams("name"),
      request.queryParams("sim_cmd"),
      request.queryParams("ver_cmd")
    );
    response.redirect(HostComponent.getUrl(host));
  }

  public enum Mode {Default, Add}
}
