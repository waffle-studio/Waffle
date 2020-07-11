package jp.tkms.waffle.component;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

public class HostsComponent extends AbstractAccessControlledComponent {
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
      if (isPost()) {
        ArrayList<Lte.FormError> errors = checkCreateProjectFormError();
        if (errors.isEmpty()) {
          addHost();
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
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

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
                Lte.formInputGroup("text", "name", null, "Name", null, errors)
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
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

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
            null, null, Html.fasIcon("plus-square")
          ),
          Lte.table("table-condensed", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("width:8em;", "ID"));
              list.add(new Lte.TableValue("", "Name"));
              list.add(new Lte.TableValue("width:8em;", "Job"));
              list.add(new Lte.TableValue("width:2em;", ""));
              return list;
            }

            @Override
            public ArrayList<Future<Lte.TableRow>> tableRows() {
              ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
              for (Host host : Host.getList()) {
                list.add(Main.threadPool.submit(() -> {
                  return new Lte.TableRow(
                    Html.a(HostComponent.getUrl(host), null, null,  host.getShortId()),
                    host.getName(),
                    String.valueOf(Job.getList(host).size()),
                    host.getState().getStatusBadge()
                  );
                }));
              }
              return list;
            }
          })
          , null, null, "p-0");
      }
    }.render(this);
  }

  private void addHost() {
    Host host = Host.create(request.queryParams("name"));
    response.redirect(HostComponent.getUrl(host));
  }

  public enum Mode {Default, Add}
}
