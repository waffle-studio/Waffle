package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ProjectComponent extends AbstractComponent {
    public static final String ROOT = "/project";

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
        Spark.get( ROOT + "/:id", new ProjectComponent());
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
            renderProjectsList();
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
                return new ArrayList<String>(Arrays.asList(Html.a(ProjectsComponent.ROOT, "Projects"), "NotFound"));
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

    private void renderProjectsList() {
        new MainTemplate() {
            @Override
            protected String pageTitle(){
                return "Project";
            }

            @Override
            protected String pageSubTitle() {
                return project.getName();
            }

            @Override
            protected ArrayList<String> pageBreadcrumb() {
                return new ArrayList<String>(Arrays.asList(Html.a(ProjectsComponent.ROOT, "Projects"), project.getId()));
            }

            @Override
            protected String pageContent() {
                ArrayList<Project> projectList = Project.getProjectList();
                if (projectList.size() <= 0) {
                    return Lte.card(null, null,
                        Html.a("/project/create", null, null,
                            Html.faIcon("plus-square") + "Create new project"
                        ),
                        null
                    );
                }
                return Lte.card(null,
                        Html.a("/project/create",
                                null, null, Html.faIcon("plus-square")
                        ),
                        Lte.table("table-condensed", getProjectTableHeader(), getProjectTableRow())
                        , null, null, "p-0");
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
                    Html.a("/project/" + project.getId(), null, null,project.getShortId()),
                    project.getName())
            );
        }
        return list;
    }

    private void createProject() {

    }
}
