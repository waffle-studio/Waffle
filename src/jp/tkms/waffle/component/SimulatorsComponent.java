package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class SimulatorsComponent extends AbstractComponent {
    public enum Mode {Default, Add};

    private Mode mode;

    private String requestedId;
    private Project project;

    public SimulatorsComponent(Mode mode) {
        super();
        this.mode = mode;
    }

    public SimulatorsComponent() {
        this(Mode.Default);
    }

    static public void register() {
        Spark.get(getUrl(), new SimulatorsComponent());
    }

    public static String getUrl(String... values) {
        return "/simulators/" + (values.length == 0 ? ":project" : values[0]);
    }

    @Override
    public void controller() {
        requestedId = request.params("project");
        project = new Project(requestedId);

        if (mode == Mode.Add) {
            if (request.requestMethod().toLowerCase().equals("post")) {
                ArrayList<Lte.FormError> errors = checkCreateProjectFormError();
                if (errors.isEmpty()) {
                    createProject();
                } else {
                    renderCreateProjectForm(errors);
                }
            } else {
                renderCreateProjectForm(new ArrayList<>());
            }
        } else {
            renderProjectsList();
        }
    }

    private void renderCreateProjectForm(ArrayList<Lte.FormError> errors) {
        new MainTemplate() {
            @Override
            protected String pageTitle() {
                return "Projects";
            }

            @Override
            protected String pageSubTitle() {
                return "Add";
            }

            @Override
            protected ArrayList<String> pageBreadcrumb() {
                return new ArrayList<String>(Arrays.asList(
                    Html.a("/projects", null, null ,"Projects"),
                    "Add")
                );
            }

            @Override
            protected String pageContent() {
                return
                    Html.form("/create", Html.Method.Post,
                        Lte.card("New Project", null,
                            Html.div(null,
                                Html.formHidden("cmd", "create"),
                                Lte.formInputGroup("text", "projectName", "Name", "", errors)
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

    private void renderProjectsList() {
        new MainTemplate() {
            @Override
            protected String pageTitle() {
                return "Simulators";
            }

            @Override
            protected ArrayList<String> pageBreadcrumb() {
                return new ArrayList<String>(Arrays.asList("Projects",
                        Html.a(ProjectComponent.getUrl(project.getId()), project.getShortId()),
                        "Simulators"));
            }

            @Override
            protected String pageContent() {
                ArrayList<Project> projectList = Project.getProjectList();
                if (projectList.size() <= 0) {
                    return Lte.card(null, null,
                        Html.a("/projects/create", null, null,
                            Html.faIcon("plus-square") + "Add new project"
                        ),
                        null
                    );
                }
                return Lte.card(null,
                        Html.a("/projects/create",
                                null, null, Html.faIcon("plus-square")
                        ),
                        Lte.table("table-condensed", getProjectTableHeader(), getProjectTableRow())
                        , null, null, "p-0");
            }
        }.render(this);
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
        String name = request.queryParams("projectName");
        Project project = Project.create(name);
        response.redirect(ProjectComponent.getUrl(project.getId()));
    }
}
