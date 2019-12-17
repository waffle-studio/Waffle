package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.*;
import spark.Spark;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Arrays;

public class SimulatorComponent extends AbstractComponent {
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

    SimulatorsComponent.register();
    TrialsComponent.register();
    ParameterModelGroupComponent.register();
    ParameterModelComponent.register();
    ParameterExtractorComponent.register();
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
      default:
        renderSimulator();
        break;
    }
  }

  private void renderSimulator() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return simulator.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          simulator.getId()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Lte.card(Html.faIcon("terminal") + "Properties", null,
          Html.form(getUrl(simulator, "update"), Html.Method.Post,
            Html.div(null,
              Lte.readonlyTextInput("Simulator Directory", simulator.getLocation().toString()),
              Lte.formInputGroup("text", "sim_cmd", "Simulation command", "", simulator.getSimulationCommand(), errors),
              Lte.formSubmitButton("primary", "Update")
            )
          )
          , null);

        ParameterModelGroup rootGroup = ParameterModelGroup.getRootInstance(simulator);

        content += Lte.card(Html.faIcon("list-alt") + "Parameter Models",
          Html.a(ParameterModelGroupComponent.getUrl(ParameterModelGroup.getRootInstance(simulator), ParameterModelGroupComponent.MODE_ADD_PARAMETER_GROUP), Html.faIcon("plus-square") + "Group") +
            "/" + Html.a(ParameterModelGroupComponent.getUrl(rootGroup, ParameterModelGroupComponent.MODE_ADD_PARAMETER), Html.faIcon("plus") + "Parameter"),
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
              for (ParameterModel model : ParameterModel.getList(rootGroup)) {
                list.add(new Lte.TableRow(
                  Html.a(ParameterModelComponent.getUrl(model), null, null, model.getShortId()),
                  model.getName())
                );
              }
              for (ParameterModelGroup group : ParameterModelGroup.getList(rootGroup)) {
                list.add(new Lte.TableRow(
                  Html.a(ParameterModelGroupComponent.getUrl(group), null, null, "*" + group.getShortId()),
                  group.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0");

        content += Lte.card(Html.faIcon("file-import") + "Parameter Extractor",
          Html.a(ParameterExtractorComponent.getStaticUrl(simulator, "add"), Html.faIcon("plus")),
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
              for (ParameterExtractor extractor : ParameterExtractor.getList(simulator)) {
                list.add(new Lte.TableRow(
                  Html.a(ParameterExtractorComponent.getUrl(extractor), null, null, extractor.getShortId()),
                  extractor.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0");

        content += Lte.card(Html.faIcon("dolly-flatbed") + "Result Collectors", null,
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
              for (ResultCollector collector : ResultCollector.getList(simulator)) {
                list.add(new Lte.TableRow(
                  Html.a("", null, null, collector.getShortId()),
                  collector.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0");

        return content;
      }
    }.render(this);
  }

  void updateSimulator() {
    simulator.setSimulatorCommand(request.queryParams("sim_cmd"));
    response.redirect(getUrl(simulator));
  }

  public enum Mode {Default, Update}
}
