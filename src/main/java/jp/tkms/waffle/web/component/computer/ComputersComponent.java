package jp.tkms.waffle.web.component.computer;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.MainTemplate;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.Job;
import jp.tkms.waffle.data.util.HostState;
import org.json.JSONObject;
import spark.Spark;

import java.util.*;
import java.util.concurrent.Future;

public class ComputersComponent extends AbstractAccessControlledComponent {
  public static final String COMPUTERS = "Computers";
  public static final String COMPUTER = "Computer";

  private static final String KEY_WORKBASE = "work_base_dir";
  private static final String KEY_XSUB = "xsub_dir";
  private static final String KEY_POLLING = "polling_interval";
  private static final String KEY_MAX_THREADS = "maximum_threads";
  private static final String KEY_ALLOCABLE_MEMORY = "allocable_memory";
  private static final String KEY_PARAMETERS = "parameters";
  private static final String KEY_ENVIRONMENTS = "environments";
  public enum Mode {Default, New, Update}
  private Mode mode = null;
  private Computer computer = null;
  private static final HashMap<String, String> submitterTypeMap = new HashMap<>();
  private static final ArrayList<String> submitterTypeList = new ArrayList<>();

  public ComputersComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ComputersComponent() {
    this(Mode.Default);
  }

  static public void register() {
    for (String fullName : Computer.getSubmitterTypeNameList()) {
      String name = fullName.replaceFirst("^jp\\.tkms\\.waffle\\.submitter\\.", "");
      submitterTypeMap.put(name, fullName);
      submitterTypeList.add(name);
    }

    Spark.get(getUrl(), new ComputersComponent());
    Spark.get(getUrl(Mode.New), new ComputersComponent(Mode.New));
    Spark.post(getUrl(Mode.New), new ComputersComponent(Mode.New));
    Spark.get(getUrl(null, null), new ComputersComponent());
    Spark.post(getUrl(null, Mode.Update), new ComputersComponent(Mode.Update));
  }

  public static String getUrl() {
    return "/COMPUTER";
  }

  public static String getUrl(Mode mode) {
    return "/COMPUTER/@" + mode.name();
  }

  public static String getUrl(Computer computer) {
    return "/COMPUTER/" + (computer == null ? ":name" : computer.getName());
  }

  public static String getUrl(Computer computer, Mode mode) {
    return getUrl(computer) + (mode == null ? "" : "/@" + mode.name());
  }

  @Override
  public void controller() {
    if (request.params("name") == null) {
      switch (mode) {
        case New:
          if (isPost()) {
            ArrayList<Lte.FormError> errors = checkCreateProjectFormError();
            if (errors.isEmpty()) {
              addComputer();
            } else {
              renderNewForm(errors);
            }
          } else {
            renderNewForm(new ArrayList<>());
          }
          break;
        default:
          renderComputerList();
      }
    } else {
      computer = Computer.find(request.params("name"));
      switch (mode) {
        case Update:
          updateComputer();
          break;
        default:
          renderComputer();
      }
    }
  }

  private void renderNewForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "@New";
      }

      @Override
      protected String pageSubTitle() {
        return COMPUTERS;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ComputersComponent.getUrl(), COMPUTERS),
          "@New"));
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(Mode.New), Html.Method.Post,
            Lte.card("New Computer", null,
              Html.div(null,
                Html.inputHidden("cmd", "new"),
                Lte.formInputGroup("text", "name", null, "Name", null, errors),
                Lte.formSelectGroup("type", "type", submitterTypeList, errors)
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

  private void renderComputerList() {
    new MainTemplate() {

      @Override
      protected boolean enableParentLink() {
        return false;
      }

      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return COMPUTERS;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          COMPUTERS));
      }

      @Override
      protected String pageTool() {
        return Html.a(getUrl(Mode.New),
            null, null, Lte.badge("primary", null, Html.fasIcon("plus-square") + "NEW")
          );
      }

      @Override
      protected String pageContent() {
        return Lte.card(null, null,
          Lte.table("table-condensed", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("", "Name"));
              list.add(new Lte.TableValue("width:8em;", "Job"));
              list.add(new Lte.TableValue("width:2em;", ""));
              return list;
            }

            @Override
            public ArrayList<Future<Lte.TableRow>> tableRows() {
              ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
              for (Computer computer : Computer.getList()) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                  return new Lte.TableRow(
                    Html.a(ComputersComponent.getUrl(computer), null, null,  computer.getName()),
                    String.valueOf(Job.getList(computer).size()),
                    computer.getState().getStatusBadge()
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

  private void addComputer() {
    Computer computer = Computer.create(request.queryParams("name"), submitterTypeMap.get(request.queryParams("type")));
    response.redirect(ComputersComponent.getUrl(computer));
  }

  private void renderComputer() {
    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return computer.getName();
      }

      @Override
      protected String pageSubTitle() {
        return COMPUTER;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ComputersComponent.getUrl(), COMPUTERS),
          computer.getName()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += ("".equals(computer.getMessage()) ? "" : Lte.errorNoticeTextAreaGroup(computer.getMessage()));
        if (computer.getState().equals(HostState.XsubNotFound)) {
          content += Lte.divRow(Lte.divCol(Lte.DivSize.F12,
            Html.a(ComputersComponent.getUrl(),
              Html.fasIcon("info-circle") + "You can install Xsub on "
                + (computer.getXsubDirectory().equals("") ? "~/xsub" : computer.getXsubDirectory()) + " from here.")));
        }
        content += Html.form(getUrl(computer, Mode.Update), Html.Method.Post,
          Lte.card(Html.fasIcon("terminal") + "Properties",
            computer.getState().getStatusBadge(),
            Html.div(null,
              Lte.readonlyTextInput("Submitter Type", computer.getSubmitterType()),
              Lte.formInputGroup("text", KEY_XSUB,
                "Xsub directory on the computer",
                "depends on $PATH", computer.getXsubDirectory(), errors),
              Lte.formInputGroup("text", KEY_WORKBASE,
                "Work base directory on the computer", "", computer.getWorkBaseDirectory(), errors),
              Lte.formInputGroup("text", KEY_MAX_THREADS,
                "Maximum number of threads", "", computer.getMaximumNumberOfThreads().toString(), errors),
              Lte.formInputGroup("text", KEY_ALLOCABLE_MEMORY,
                "Allocable memory size (GB)", "", computer.getAllocableMemorySize().toString(), errors),
              Lte.formInputGroup("text", KEY_POLLING,
                "Polling interval (seconds)", "", computer.getPollingInterval().toString(), errors),
              Lte.formJsonEditorGroup(KEY_ENVIRONMENTS, "Environments", "tree", computer.getEnvironments().toString(), null),
              Lte.formJsonEditorGroup(KEY_PARAMETERS, "Parameters", "tree",  computer.getParametersWithDefaultParametersFiltered().toString(), null)
            )
            , Lte.formSubmitButton("success", "Update")
          )
        );

        return content;
      }
    }.render(this);
  }

  private void updateComputer() {
    computer.setXsubDirectory(request.queryParams(KEY_XSUB));
    computer.setWorkBaseDirectory(request.queryParams(KEY_WORKBASE));
    computer.setMaximumNumberOfThreads(Double.parseDouble(request.queryParams(KEY_MAX_THREADS)));
    computer.setAllocableMemorySize(Double.parseDouble(request.queryParams(KEY_ALLOCABLE_MEMORY)));
    computer.setPollingInterval(Integer.parseInt(request.queryParams(KEY_POLLING)));
    computer.setEnvironments(new JSONObject(request.queryParams(KEY_ENVIRONMENTS)));
    computer.setParameters(new JSONObject(request.queryParams(KEY_PARAMETERS)));
    computer.update();
    response.redirect(getUrl(computer));
  }
}
