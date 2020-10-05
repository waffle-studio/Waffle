package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Project;
import org.json.JSONObject;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class HostComponent extends AbstractAccessControlledComponent {
  private static final String KEY_WORKBASE = "work_base_dir";
  private static final String KEY_XSUB = "xsub_dir";
  private static final String KEY_POLLING = "polling_interval";
  private static final String KEY_MAX_NODES = "maximum_nodes";
  private static final String KEY_MAX_THREADS = "maximum_threads";
  private static final String KEY_ALLOCABLE_MEMORY = "allocable_memory";
  private static final String KEY_PARAMETERS = "parameters";
  private static final String KEY_ENVIRONMENTS = "environments";
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
    Spark.post(getUrl(null, "update"), new HostComponent(Mode.Update));
  }

  public static String getUrl(Host host) {
    return "/host/" + (host == null ? ":name" : host.getName());
  }

  public static String getUrl(Host host, String mode) {
    return getUrl(host) + '/' + mode;
  }

  @Override
  public void controller() {
    host = Host.find(request.params("name"));
    switch (mode) {
      case Update:
        updateHost();
        break;
      default:
        renderHost();
    }
  }

  private void renderHost() {
    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "Host";
      }

      @Override
      protected String pageSubTitle() {
        return host.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(HostsComponent.getUrl(), "Hosts"),
          host.getName()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Html.form(getUrl(host, "update"), Html.Method.Post,
          Lte.card(Html.fasIcon("terminal") + "Properties",
            host.getState().getStatusBadge(),
            Html.div(null,
              Lte.readonlyTextInput("Submitter Type", host.getSubmitterType()),
              Lte.formInputGroup("text", KEY_XSUB,
                "Xsub directory on host",
                "depends on $PATH", host.getXsubDirectory(), errors),
              Lte.formInputGroup("text", KEY_WORKBASE,
                "Work base directory on host", "", host.getWorkBaseDirectory(), errors),
              Lte.formInputGroup("text", KEY_MAX_THREADS,
                "Maximum number of therads", "", host.getMaximumNumberOfThreads().toString(), errors),
              Lte.formInputGroup("text", KEY_ALLOCABLE_MEMORY,
                "Allocable memory size (GB)", "", host.getAllocableMemorySize().toString(), errors),
              Lte.formInputGroup("text", KEY_POLLING,
                "Polling interval (seconds)", "", host.getPollingInterval().toString(), errors),
              Lte.formJsonEditorGroup(KEY_ENVIRONMENTS, "Environments", "tree", host.getEnvironments().toString(), null),
              Lte.formJsonEditorGroup(KEY_PARAMETERS, "Parameters", "tree",  host.getParametersWithDefaultParametersFiltered().toString(), null)
            )
            , Lte.formSubmitButton("success", "Update")
          )
        );

        return content;
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private ArrayList<Lte.TableValue> getProjectTableHeader() {
    ArrayList<Lte.TableValue> list = new ArrayList<>();
    list.add(new Lte.TableValue("", "Name"));
    return list;
  }

  private ArrayList<Lte.TableRow> getProjectTableRow() {
    ArrayList<Lte.TableRow> list = new ArrayList<>();
    for (Project project : Project.getList()) {
      list.add(new Lte.TableRow(
        Html.a("", null, null, project.getName())
        )
      );
    }
    return list;
  }

  private void updateHost() {
    host.setXsubDirectory(request.queryParams(KEY_XSUB));
    host.setWorkBaseDirectory(request.queryParams(KEY_WORKBASE));
    host.setMaximumNumberOfNodes(Integer.parseInt(request.queryParams(KEY_MAX_NODES)));
    host.setMaximumNumberOfThreads(Double.parseDouble(request.queryParams(KEY_MAX_THREADS)));
    host.setAllocableMemorySize(Double.parseDouble(request.queryParams(KEY_ALLOCABLE_MEMORY)));
    host.setPollingInterval(Integer.parseInt(request.queryParams(KEY_POLLING)));
    host.setEnvironments(new JSONObject(request.queryParams(KEY_ENVIRONMENTS)));
    host.setParameters(new JSONObject(request.queryParams(KEY_PARAMETERS)));
    host.update();
    response.redirect(getUrl(host));
  }

  public enum Mode {Default, Update}
}
