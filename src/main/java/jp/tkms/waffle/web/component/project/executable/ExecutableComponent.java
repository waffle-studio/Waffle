package jp.tkms.waffle.web.component.project.executable;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.component.computer.ComputersComponent;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.project.workspace.run.RunComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import jp.tkms.waffle.exception.RunNotFoundException;
import spark.Spark;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ExecutableComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "Executable";
  private static final String KEY_DEFAULT_PARAMETERS = "default_parameters";
  private static final String KEY_DUMMY_RESULTS = "dummy_results";
  private static final String KEY_PARAMETERS = "parameters";
  private static final String KEY_COMPUTER = "computer";
  protected static final String KEY_EXECUTABLE = "executable";

  protected Mode mode;

  protected Project project;
  protected Executable executable;
  public ExecutableComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ExecutableComponent() {
    this(Mode.Default);
  }

  public static void register() {
    Spark.get(getUrl(null), new ExecutableComponent());
    Spark.post(getUrl(null, Mode.Update), new ExecutableComponent(Mode.Update));
    Spark.post(getUrl(null, Mode.UpdateDefaultParameters), new ExecutableComponent(Mode.UpdateDefaultParameters));
    Spark.post(getUrl(null, Mode.UpdateDummyResults), new ExecutableComponent(Mode.UpdateDummyResults));
    Spark.get(getUrl(null, Mode.TestRun), new ExecutableComponent(Mode.TestRun));
    Spark.post(getUrl(null, Mode.TestRun), new ExecutableComponent(Mode.TestRun));

    ParameterExtractorComponent.register();
    ResultCollectorComponent.register();
  }

  public static String getUrl(Executable executable) {
    return ExecutablesComponent.getUrl(executable == null ? null : executable.getProject())
      + (executable == null ? "/:" + KEY_EXECUTABLE : '/' + executable.getName());
  }

  public static String getUrl(Executable executable, Mode mode) {
    return getUrl(executable) + "/@" + mode.name();
  }

  protected Executable getExecutableEntity() {
    return Executable.getInstance(project, request.params(KEY_EXECUTABLE));
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));
    executable = getExecutableEntity();

    switch (mode) {
      case Update: {
        executable.setCommand(request.queryParams("sim_cmd"));
        executable.setRequiredThread(Double.parseDouble(request.queryParams("req_t")));
        executable.setRequiredMemory(Double.parseDouble(request.queryParams("req_m")));
        response.redirect(getUrl(executable));
        break;
      }
      case UpdateDefaultParameters: {
        executable.setDefaultParameters(request.queryParams(KEY_DEFAULT_PARAMETERS));
        response.redirect(getUrl(executable));
        break;
      }
      case UpdateDummyResults: {
        executable.setDefaultParameters(request.queryParams(KEY_DUMMY_RESULTS));
        response.redirect(getUrl(executable));
        break;
      }
      case TestRun: {
        if (isPost()) {
          runExecutableAsTestRun();
        }
        renderTestRun();
        break;
      }
      default:
        renderExecutable();
        break;
    }
  }

  protected String renderSubTitle() {
    return TITLE;
  }

  protected ArrayList<String> renderPageBreadcrumb() {
    return new ArrayList<String>(Arrays.asList(
      Html.a(ProjectsComponent.getUrl(), "Projects"),
      Html.a(ProjectComponent.getUrl(project), project.getName()),
      Html.a(ExecutablesComponent.getUrl(project), ExecutablesComponent.EXECUTABLES)
    ));
  }

  protected String renderTool() {
    return Html.a(getUrl(executable, Mode.TestRun),
      Html.span("right badge badge-secondary", null, "TEST RUN")
    );
  }

  private void renderExecutable() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return executable.getName();
      }

      @Override
      protected String pageSubTitle() {
        return renderSubTitle();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return renderPageBreadcrumb();
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content +=
          Html.form(getUrl(executable, Mode.Update), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "Properties",
              renderTool(),
              Html.div(null,
                Lte.readonlyTextInputWithCopyButton("Executable Bin Directory (BASE)", executable.getBaseDirectory().toString()),
                //Lte.readonlyTextInput("Version ID", executable.getVersionId()),
                Lte.formInputGroup("text", "sim_cmd", "Simulator command", "", executable.getCommand(), errors),
                Lte.formInputGroup("text", "req_t", "Required thread", "", executable.getRequiredThread().toString(), errors),
                Lte.formInputGroup("text", "req_m", "Required memory (GB)", "", executable.getRequiredMemory().toString(), errors)
              ),
              Lte.formSubmitButton("success", "Update"),
              "card-outline card-info"
              , null
            )
          );


        /*
        ParameterGroup rootGroup = ParameterGroup.getRootInstance(simulator);

        content += Lte.card(Html.faIcon("list-alt") + "Parameter Models",
          Html.a(ParameterGroupComponent.getUrl(ParameterGroup.getRootInstance(simulator), ParameterGroupComponent.MODE_ADD_PARAMETER_GROUP), Html.faIcon("plus-square") + "Group") +
            "/" + Html.a(ParameterGroupComponent.getUrl(rootGroup, ParameterGroupComponent.MODE_ADD_PARAMETER), Html.faIcon("plus") + "Parameter"),
          Lte.table(null, new Lte.Table() {
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
              for (Parameter model : Parameter.getList(rootGroup)) {
                list.add(new Lte.TableRow(
                  Html.a(ParameterComponent.getUrl(model), null, null, model.getShortId()),
                  model.getName())
                );
              }
              for (ParameterGroup group : ParameterGroup.getList(rootGroup)) {
                list.add(new Lte.TableRow(
                  Html.a(ParameterGroupComponent.getUrl(group), null, null, "*" + group.getShortId()),
                  group.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0");

         */

        //String defaultParametersText = executable.getDefaultParameters().toString(2);

        content += Lte.card(Html.fasIcon("file-import") + "Parameter Extractors",
          Html.a(ParameterExtractorComponent.getStaticUrl(executable, ParameterExtractorComponent.Mode.Add), Lte.badge("primary", null,  Html.fasIcon("plus-square") + "NEW")),
          Lte.table(null, new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              return null;
            }

            @Override
            public ArrayList<Future<Lte.TableRow>> tableRows() {
              ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
              List<String> nameList = executable.getExtractorNameList();
              if (nameList != null) {
                for (String extractorName : nameList) {
                  list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(
                        Html.a(ParameterExtractorComponent.getUrl(executable, extractorName), null, null, Html.fasIcon("file") + extractorName));
                    }
                  ));
                }
              }
              if (list.isEmpty()) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(new Lte.TableValue("text-align:center;color:silver;", Html.fasIcon("receipt") + "Empty"));
                  }
                ));
              }
              return list;
            }
          })
          , null, "card-secondary card-outline", "p-0");

        content += Lte.card(Html.fasIcon("dolly-flatbed") + "Result Collectors",
          Html.a(ResultCollectorComponent.getStaticUrl(executable, ResultCollectorComponent.Mode.Add), Lte.badge("primary", null, Html.fasIcon("plus-square") + "NEW")),
          Lte.table(null, new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              return null;
            }

            @Override
            public ArrayList<Future<Lte.TableRow>> tableRows() {
              ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
              List<String> nameList = executable.getCollectorNameList();
              if (nameList != null) {
                for (String collectorName : nameList) {
                  list.add(Main.interfaceThreadPool.submit(() -> {
                      return new Lte.TableRow(
                        Html.a(ResultCollectorComponent.getUrl(executable, collectorName), null, null, Html.fasIcon("file") + collectorName));
                    }
                  ));
                }
              }
              if (list.isEmpty()) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(new Lte.TableValue("text-align:center;color:silver;", Html.fasIcon("receipt") + "Empty"));
                  }
                ));
              }
              return list;
            }
          })
          , null, "card-secondary card-outline", "p-0");

        content +=
          Html.form(getUrl(executable, Mode.UpdateDefaultParameters), Html.Method.Post,
            Lte.card(Html.fasIcon("list-ol") + "Default Parameters",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formJsonEditorGroup(KEY_DEFAULT_PARAMETERS, null, "tree", executable.getDefaultParameters().toString(), null)
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop card-secondary card-outline", null)
          );

        content +=
          Html.form(getUrl(executable, Mode.UpdateDummyResults), Html.Method.Post,
            Lte.card(Html.fasIcon("list-ol") + "Dummy Results",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formJsonEditorGroup(KEY_DUMMY_RESULTS, null, "tree", executable.getDummyResults().toString(), null)
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop card-secondary card-outline", null)
          );

        content += Lte.card(Html.fasIcon("file") + "Files in Executable Bin Directory (BASE)",
          Lte.cardToggleButton(false),
          Lte.table("table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              return null;
            }

            @Override
            public ArrayList<Future<Lte.TableRow>> tableRows() {
              ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
              for (File child : executable.getBaseDirectory().toFile().listFiles()) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                  return new Lte.TableRow(child.getName());
                  }
                ));
              }
              if (list.isEmpty()) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(new Lte.TableValue("text-align:center;color:silver;", Html.fasIcon("receipt") + "Empty"));
                  }
                ));
              }
              return list;
            }
          })
          , null, "collapsed-card.stop card-secondary card-outline", "p-0");

        return content;
      }
    }.render(this);
  }

  private void renderTestRun() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return executable.getName();
      }

      @Override
      protected String pageSubTitle() {
        return "Test Run";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return renderPageBreadcrumb();
      }

      @Override
      protected String pageContent() {
        String content = "";

        ExecutableRun latestRun = null;
        try {
          latestRun = executable.getLatestTestRun();
        } catch (RunNotFoundException e) { }

        if (latestRun != null) {

          ExecutableRun finalLatestRun = latestRun;
          content += Lte.card(Html.fasIcon("poll-h") + "Latest Test Run", null,
            Lte.table("table-condensed table-sm", new Lte.Table() {
              @Override
              public ArrayList<Lte.TableValue> tableHeaders() {
                ArrayList<Lte.TableValue> list = new ArrayList<>();
                list.add(new Lte.TableValue("", "Name"));
                list.add(new Lte.TableValue("", "Computer"));
                list.add(new Lte.TableValue("width:2em;", ""));
                return list;
              }

              @Override
              public ArrayList<Future<Lte.TableRow>> tableRows() {
                ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
                list.add(Main.interfaceThreadPool.submit(() -> {
                  return new Lte.TableRow(
                    Html.a(RunComponent.getUrl(finalLatestRun), finalLatestRun.getName()),
                    (finalLatestRun.getComputer() == null ? "NotFound" :
                      Html.a(
                        ComputersComponent.getUrl(finalLatestRun.getComputer()),
                        finalLatestRun.getComputer().getName()
                      )
                    ),
                    Html.spanWithId(finalLatestRun.getLocalDirectoryPath().toString() + "-badge", finalLatestRun.getState().getStatusBadge())
                  );
                }));
                return list;
              }
            })
            , null, null, "p-0");
        }

        content +=
          Html.form(getUrl(executable, Mode.TestRun), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "Test Run",
              null,
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formSelectGroup(KEY_COMPUTER, ComputersComponent.COMPUTER, Computer.getViableList().stream().map(computer -> computer.getName()).collect(Collectors.toList()), null),
                  Lte.formJsonEditorGroup(KEY_PARAMETERS, "Parameters", "tree", executable.getDefaultParameters().toString(2), null)
                )
              )
              ,Lte.formSubmitButton("primary", "Run")
            )
          );

        return content;
      }
    }.render(this);
  }

  void runExecutableAsTestRun() {
    ExecutableRun run = executable.postTestRun(Computer.find(request.queryParams(KEY_COMPUTER)), request.queryParams(KEY_PARAMETERS));
    response.redirect(RunComponent.getUrl(run));
  }

  public enum Mode {Default, Update, UpdateDefaultParameters, UpdateDummyResults, TestRun}
}
