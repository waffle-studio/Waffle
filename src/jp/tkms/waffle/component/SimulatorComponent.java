package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.Simulator;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class SimulatorComponent extends AbstractComponent {
    public enum Mode {Default, EditConstModel};

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
        Spark.get(getUrl(null) , new SimulatorComponent());
        Spark.get(getUrl(null, "edit_const_model") , new SimulatorComponent());

        SimulatorsComponent.register();
        TrialsComponent.register();
    }

    public static String getUrl(Simulator simulator) {
        return "/simulator/" + (simulator == null ? ":project/:id" : simulator.getProject().getId() + '/' + simulator.getId());
    }

    public static String getUrl(Simulator simulator, String mode) {
        return getUrl(simulator) + '/' + mode;
    }

    @Override
    public void controller() {
        project = new Project(request.params("project"));
        if (!project.isValid()) {
        }

        simulator = project.getSimulator(request.params("id"));

        renderSimulator();
    }

    private void renderSimulator() {
        new MainTemplate() {
            @Override
            protected String pageTitle(){
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

                content += Lte.card(Html.faIcon("terminal") + "Basic",
                    Html.a("", Html.faIcon("edit")),
                    Html.div(null,
                        Html.div(null,
                            "Simulation Command",
                            Lte.disabledTextInput(simulator.getSimulationCommand())
                        ),
                        Html.div(null,
                            "Version Command",
                            Lte.disabledTextInput(simulator.getVersionCommand())
                        )
                    )
                    ,null);

                content += Lte.divRow(
                    Lte.infoBox(Lte.DivSize.F12Md12Sm6,"file-import", "",
                        Html.a(SimulatorsComponent.getUrl(project), "Parameter extractor"), ""),
                    Lte.infoBox(Lte.DivSize.F12Md12Sm6,"pencil-ruler", "",
                        Html.a(TrialsComponent.getUrl(project), "Parameter modifier"), "")
                );

                content += Lte.card(Html.faIcon("list-alt") + "Parameter models", null, null, null);

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
        for (Project project : Project.getProjectList()) {
            list.add(new Lte.TableRow(
                    Html.a("", null, null,project.getShortId()),
                    project.getName())
            );
        }
        return list;
    }

    private void createProject() {

    }
}
