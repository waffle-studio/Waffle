package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.ConductorTemplate;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class ConductorTemplatesComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "ConductorTemplates";

  private Mode mode;

  public ConductorTemplatesComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ConductorTemplatesComponent() {
    this(Mode.Default);
  }

  public static void register() {
    Spark.get(getUrl(), new ConductorTemplatesComponent());
    Spark.get(getUrl("add"), new ConductorTemplatesComponent(ConductorTemplatesComponent.Mode.Add));
    Spark.post(getUrl("add"), new ConductorTemplatesComponent(ConductorTemplatesComponent.Mode.Add));

    ConductorTemplateComponent.register();
  }

  public static String getUrl() {
    return getUrl("");
  }

  public static String getUrl(String mode) {
    return "/conductor-templates/" + mode;
  }

  @Override
  public void controller() {
    if (mode == Mode.Add) {
      if (request.requestMethod().toLowerCase().equals("post")) {
        ArrayList<Lte.FormError> errors = checkAddFormError();
        if (errors.isEmpty()) {
          addConductorModule();
        } else {
          renderAddForm(errors);
        }
      } else {
        renderAddForm(new ArrayList<>());
      }
    } else {
      renderProjectList();
    }
  }

  private void renderAddForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return TITLE;
      }

      @Override
      protected String pageSubTitle() {
        return "Add";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(getUrl(), null, null, TITLE),
          "Add")
        );
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl("add"), Html.Method.Post,
            Lte.card("New ConductorTemplate", null,
              Html.div(null,
                Html.inputHidden("cmd", "add"),
                Lte.formInputGroup("text", "name", null, "Name", null, errors)
              ),
              Lte.formSubmitButton("success", "Add"),
              "card-warning", null
            )
          );
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkAddFormError() {
    return new ArrayList<>();
  }

  private void renderProjectList() {
    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return TITLE;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(TITLE));
      }

      @Override
      protected String pageContent() {
        ArrayList<ConductorTemplate> moduleList = ConductorTemplate.getList();
        if (moduleList.size() <= 0) {
          return Lte.card(null, null,
            Html.a(getUrl("add"), null, null,
              Html.faIcon("plus-square") + "Add ConductorTemplate"
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
              for (ConductorTemplate module : moduleList) {
                list.add(new Lte.TableRow(
                  Html.a(ConductorTemplateComponent.getUrl(module), null, null, module.getShortId()),
                  module.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0");
      }
    }.render(this);
  }

  private void addConductorModule() {
    String name = request.queryParams("name");
    ConductorTemplate module = ConductorTemplate.create(name);
    response.redirect(ConductorTemplateComponent.getUrl(module));
  }

  public enum Mode {Default, Add}
}
