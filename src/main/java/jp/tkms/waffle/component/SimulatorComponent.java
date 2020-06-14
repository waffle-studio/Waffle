package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.data.*;
import spark.Spark;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SimulatorComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "Simulators";
  private static final String KEY_DEFAULT_PARAMETERS = "default_parameters";
  private static final String KEY_UPDATE_PARAMETERS = "update-parameters";
  private static final String KEY_PARAMETERS = "parameters";
  private static final String KEY_RUN = "run";
  private static final String KEY_HOST = "host";

  private Mode mode;

  private Project project;
  private Simulator simulator;
  public SimulatorComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public SimulatorComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new SimulatorComponent());
    Spark.post(getUrl(null, "update"), new SimulatorComponent(Mode.Update));
    Spark.post(getUrl(null, KEY_UPDATE_PARAMETERS), new SimulatorComponent(Mode.UpdateParameters));
    Spark.get(getUrl(null, KEY_RUN), new SimulatorComponent(Mode.TestRun));
    Spark.post(getUrl(null, KEY_RUN), new SimulatorComponent(Mode.TestRun));

    ParameterExtractorComponent.register();
    ResultCollectorComponent.register();
  }

  public static String getUrl(Simulator simulator) {
    return "/simulator/"
      + (simulator == null ? ":project/:id" : simulator.getProject().getId() + '/' + simulator.getId());
  }

  public static String getUrl(Simulator simulator, String mode) {
    return getUrl(simulator) + '/' + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    simulator = Simulator.getInstance(project, request.params("id"));

    switch (mode) {
      case Update:
        updateSimulator();
        break;
      case UpdateParameters:
        updateDefaultParameters();
        break;
      case TestRun:
        if (isPost()) {
          runSimulator();
        }
        renderTestRun();
        break;
      default:
        renderSimulator();
        break;
    }
  }

  private void renderSimulator() {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return simulator.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          simulator.getName()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content +=
          Html.form(getUrl(simulator, "update"), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "Properties",
              Html.a(getUrl(simulator, KEY_RUN),
                Html.span("right badge badge-secondary", null, "test run")
              ),
              Html.div(null,
                Lte.readonlyTextInput("Simulator Bin Directory", simulator.getBinDirectory().toString()),
                Lte.formInputGroup("text", "sim_cmd", "Simulator command", "", simulator.getSimulationCommand(), errors)
              ),
              Lte.formSubmitButton("success", "Update")
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

        String defaultParametersText = simulator.getDefaultParameters().toString(2);

        content +=
          Html.form(getUrl(simulator, KEY_UPDATE_PARAMETERS), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "Default Parameters",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formTextAreaGroup(KEY_DEFAULT_PARAMETERS, null, defaultParametersText.split("\\n").length, defaultParametersText, null)
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop", null)
          );

        content += Lte.card(Html.faIcon("file-import") + "Parameter Extractors",
          Html.a(ParameterExtractorComponent.getStaticUrl(simulator, "add"), Html.faIcon("plus-square")),
          Lte.table(null, new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              return null;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              List<String> nameList = simulator.getExtractorNameList();
              if (nameList != null) {
                for (String extractorName : nameList) {
                  list.add(new Lte.TableRow(
                    Html.a(ParameterExtractorComponent.getUrl(simulator, extractorName), null, null, extractorName))
                  );
                }
              }
              return list;
            }
          })
          , null, null, "p-0");

        content += Lte.card(Html.faIcon("dolly-flatbed") + "Result Collectors",
          Html.a(ResultCollectorComponent.getStaticUrl(simulator, "add"), Html.faIcon("plus-square")),
          Lte.table(null, new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              return null;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              List<String> nameList = simulator.getCollectorNameList();
              if (nameList != null) {
                for (String collectorName : nameList) {
                  list.add(new Lte.TableRow(
                    Html.a(ResultCollectorComponent.getUrl(simulator, collectorName), null, null, collectorName))
                  );
                }
              }
              return list;
            }
          })
          , null, null, "p-0");

        content += Lte.card(Html.faIcon("file") + "Files in REMOTE",
          Lte.cardToggleButton(false),
          Lte.table("table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              return null;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (File child : simulator.getBinDirectory().toFile().listFiles()) {
                list.add(new Lte.TableRow(
                  child.getName())
                );
              }
              return list;
            }
          })
          , null, "collapsed-card.stop", "p-0");

        return content;
      }
    }.render(this);
  }

  private void renderTestRun() {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return simulator.getName();
      }

      @Override
      protected String pageSubTitle() {
        return "TestRun";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          "Simulators",
          simulator.getName()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        String parametersText = simulator.getDefaultParameters().toString(2);
        int parameterLines = parametersText.split("\n").length;

        SimulatorRun latestRun = simulator.getLatestTestRun();

        if (latestRun != null) {

          content += Lte.card(Html.faIcon("poll-h") + "Latest Run", null,
            Lte.table("table-condensed table-sm", new Lte.Table() {
              @Override
              public ArrayList<Lte.TableValue> tableHeaders() {
                ArrayList<Lte.TableValue> list = new ArrayList<>();
                list.add(new Lte.TableValue("width:6.5em;", "ID"));
                list.add(new Lte.TableValue("", "Host"));
                list.add(new Lte.TableValue("width:2em;", ""));
                return list;
              }

              @Override
              public ArrayList<Lte.TableRow> tableRows() {
                ArrayList<Lte.TableRow> list = new ArrayList<>();
                list.add(new Lte.TableRow(
                  Html.a(RunComponent.getUrl(project, latestRun), latestRun.getShortId()),
                  (latestRun.getHost() == null ? "NotFound" : latestRun.getHost().getName()),
                  Html.spanWithId(latestRun.getId() + "-badge", latestRun.getState().getStatusBadge())
                ));
                return list;
              }
            })
            , null, null, "p-0");
        }

        content +=
          Html.form(getUrl(simulator, "run"), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "TestRun",
              null,
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formSelectGroup(KEY_HOST, "Host", Host.getViableList().stream().map(host -> host.getName()).collect(Collectors.toList()), null),
                  Lte.formTextAreaGroup(KEY_PARAMETERS, "Parameters", parameterLines < 5 ? 5 : parameterLines, simulator.getDefaultParameters().toString(2), null)
                )
              )
              ,Lte.formSubmitButton("primary", "Run")
            )
          );

        return content;
      }
    }.render(this);
  }

  void runSimulator() {
    SimulatorRun run = simulator.runTest(Host.getInstance(request.queryParams(KEY_HOST)), request.queryParams(KEY_PARAMETERS));
    response.redirect(RunComponent.getUrl(project, run));
  }

  void updateSimulator() {
    simulator.setSimulatorCommand(request.queryParams("sim_cmd"));
    response.redirect(getUrl(simulator));
  }

  void updateDefaultParameters() {
    simulator.setDefaultParameters(request.queryParams(KEY_DEFAULT_PARAMETERS));
    response.redirect(getUrl(simulator));
  }

  public enum Mode {Default, Update, UpdateParameters, TestRun}
}
