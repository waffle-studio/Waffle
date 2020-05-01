package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class TemplatesComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  public TemplatesComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public TemplatesComponent() {
    this(Mode.Default);
  }

  public static void register() {
    Spark.get(getUrl(), new TemplatesComponent());

    ProjectComponent.register();
  }

  public static String getUrl() {
    return "/templates";
  }

  public static String getUrl(String mode) {
    return "/templates/" + mode;
  }

  @Override
  public void controller() {
    renderProjectList();
  }

  private void renderProjectList() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Templates";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList("Projects"));
      }

      @Override
      protected String pageContent() {
        ArrayList<Project> projectList = Project.getList();
        if (projectList.size() <= 0) {
          return Lte.card(null, null,
            Html.a(getUrl("add"), null, null,
              Html.faIcon("plus-square") + "Add new project"
            ),
            null
          );
        }

        return Lte.card(null,
          Html.a(getUrl("add"),
            null, null, Html.faIcon("plus-square")
          ),
          Lte.table("table-condensed", new Lte.Table() {
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
              for (Project project : Project.getList()) {
                list.add(new Lte.TableRow(
                  Html.a(ProjectComponent.getUrl(project), null, null, project.getShortId()),
                  project.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0");
      }
    }.render(this);
  }

  public enum Mode {Default}
}
