package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ProjectComponent extends AbstractComponent {
    public enum Mode {Default, NotFound};

    private Mode mode;
    private String requestedId;
    private Project project;

    public ProjectComponent(Mode mode) {
        super();
        this.mode = mode;
    }

    public ProjectComponent() {
        this(Mode.Default);
    }

    static public void register() {
        Spark.get(getUrl() , new ProjectComponent());

        SimulatorsComponent.register();
        TrialsComponent.register();
    }

    public static String getUrl(String... values) {
        return "/project/" + (values.length == 0 ? ":id" : values[0]);
    }

    @Override
    public void controller() {
        requestedId = request.params("id");
        project = new Project(requestedId);

        if (!project.isValid()) {
            mode = Mode.NotFound;
        }

        if (mode == Mode.NotFound) {
            renderProjectNotFound();
        } else {
            renderProject();
        }
    }

    private void renderProjectNotFound() {
        new MainTemplate() {
            @Override
            protected String pageTitle(){
                return "Project";
            }

            @Override
            protected String pageSubTitle() {
                return "[" + requestedId + "]";
            }

            @Override
            protected ArrayList<String> pageBreadcrumb() {
                return new ArrayList<String>(Arrays.asList(
                        Html.a(ProjectsComponent.getUrl(), "Projects"), "NotFound"));
            }

            @Override
            protected String pageContent() {
                ArrayList<Project> projectList = Project.getProjectList();
                return Lte.card(null, null,
                        Html.h1("text-center", Html.faIcon("question")),
                        null
                );
            }
        }.render(this);
    }

    private void renderProject() {
        new MainTemplate() {
            @Override
            protected String pageTitle(){
                return project.getName();
            }

            @Override
            protected ArrayList<String> pageBreadcrumb() {
                return new ArrayList<String>(Arrays.asList(
                        Html.a(ProjectsComponent.getUrl(), "Projects"), project.getId()));
            }

            @Override
            protected String pageContent() {
                String content = Lte.divRow(
                        Lte.infoBox(Lte.DivSize.F12Md12Sm6,"layer-group", "bg-info",
                                Html.a(SimulatorsComponent.getUrl(project.getId()), "Simulators"), ""),
                        Lte.infoBox(Lte.DivSize.F12Md12Sm6,"project-diagram", "bg-danger",
                                Html.a(TrialsComponent.getUrl(project.getId()), "Trials"), "")
                );

                content += Lte.card(Html.faIcon("user-tie") + "Conductors", null, null, null);

                content += Lte.card(Html.faIcon("list") + "Constant set", null, null, null);

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