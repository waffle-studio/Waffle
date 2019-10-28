package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ProjectsComponent extends AbstractComponent {
    public enum Mode {Default, Create};

    private Mode mode;

    public ProjectsComponent(Mode mode) {
        super();
        this.mode = mode;
    }

    public ProjectsComponent() {
        this(Mode.Default);
    }

    static public void register() {
        Spark.get("/projects", new ProjectsComponent());
        Spark.get("/projects/create", new ProjectsComponent(ProjectsComponent.Mode.Create));
        Spark.post("/projects/create", new ProjectsComponent(ProjectsComponent.Mode.Create));
    }

    @Override
    public void controller() {
        if (mode == Mode.Create) {
            if (request.requestMethod().toLowerCase().equals("post")) {
                ArrayList<Lte.FormError> errors = checkCreateProjectFormError();
                if (errors.isEmpty()) {
                    createProject();
                    response.redirect("/projects");
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
                return "Create";
            }

            @Override
            protected ArrayList<String> pageBreadcrumb() {
                return new ArrayList<String>(Arrays.asList(
                    Html.a("/projects", null, null ,"Projects"),
                    "Create")
                );
            }

            @Override
            protected String pageContent() {
                return
                    Html.form("/projects/create", Html.Method.Post,
                        Lte.card("New Project", null,
                            Html.div(null,
                                Html.formHidden("cmd", "create"),
                                Lte.formInputGroup("text", "projectName", "Name", "", errors)
                            ),
                            Lte.formSubmitButton("success", "Create"),
                            "card-warning", null
                        )
                    );
            }
        }.render(this);
    }

    private void renderProjectsList() {
        new MainTemplate() {
            @Override
            protected String pageTitle() {
                return "Projects";
            }

            @Override
            protected ArrayList<String> pageBreadcrumb() {
                return new ArrayList<String>(Arrays.asList("Projects"));
            }

            @Override
            protected String pageContent() {
                ArrayList<Project> projectList = Project.getProjectList();
                if (projectList.size() <= 0) {
                    return Lte.card(null, null,
                        Html.a("/projects/create", null, null,
                            Html.faIcon("plus-square") + "Create new project"
                        ),
                        null
                    );
                }
                return Lte.card(null, null, null, null);
            }
        }.render(this);
    }

    private ArrayList<Lte.FormError> checkCreateProjectFormError() {
        return new ArrayList<>();
    }

    private void createProject() {

    }
}
