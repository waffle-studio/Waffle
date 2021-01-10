package jp.tkms.waffle.web.component.template;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.MainTemplate;
import jp.tkms.waffle.data.template.ConductorTemplate;
import jp.tkms.waffle.data.template.ListenerTemplate;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

public class TemplatesComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "Templates";
  public static final String KEY_ADD_CONDUCTOR = "add_conductor";
  public static final String KEY_ADD_LISTENER = "add_listener";
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
    Spark.get(getUrl(KEY_ADD_CONDUCTOR), new TemplatesComponent(Mode.AddConductor));
    Spark.post(getUrl(KEY_ADD_CONDUCTOR), new TemplatesComponent(Mode.AddConductor));
    Spark.get(getUrl(KEY_ADD_LISTENER), new TemplatesComponent(Mode.AddListener));
    Spark.post(getUrl(KEY_ADD_LISTENER), new TemplatesComponent(Mode.AddListener));

    ConductorTemplateComponent.register();
    ListenerTemplateComponent.register();
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
    if (mode == Mode.AddConductor) {
      if (request.requestMethod().toLowerCase().equals("post")) {
        ArrayList<Lte.FormError> errors = checkAddConductorFormError();
        if (errors.isEmpty()) {
          addConductorTemplate();
        } else {
          renderAddConductorForm(errors);
        }
      } else {
        renderAddConductorForm(new ArrayList<>());
      }
    } else if (mode == Mode.AddListener) {
      if (request.requestMethod().toLowerCase().equals("post")) {
        ArrayList<Lte.FormError> errors = checkAddListenerFormError();
        if (errors.isEmpty()) {
          addListenerTemplate();
        } else {
          renderAddListenerForm(errors);
        }
      } else {
        renderAddListenerForm(new ArrayList<>());
      }
    } else {
      renderProjectList();
    }
  }

  private ArrayList<Lte.FormError> checkAddConductorFormError() {
    return new ArrayList<>();
  }

  private ArrayList<Lte.FormError> checkAddListenerFormError() {
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
        String contents = "";

        ArrayList<ConductorTemplate> conductorTemplateList = ConductorTemplate.getList();
        if (conductorTemplateList.size() <= 0) {
          contents += Lte.card(null, null,
            Html.a(getUrl(KEY_ADD_CONDUCTOR), null, null,
              Html.fasIcon("plus-square") + "Add ConductorTemplate"
            ),
            null
          );
        } else {
          contents += Lte.card("ConductorTemplate",
            Html.a(getUrl(KEY_ADD_CONDUCTOR),
              null, null, Html.fasIcon("plus-square")
            ),
            Lte.table("table-condensed", new Lte.Table() {
              @Override
              public ArrayList<Lte.TableValue> tableHeaders() {
                ArrayList<Lte.TableValue> list = new ArrayList<>();
                //list.add(new Lte.TableValue("", "Name"));
                return list;
              }

              @Override
              public ArrayList<Future<Lte.TableRow>> tableRows() {
                ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
                for (ConductorTemplate module : conductorTemplateList) {
                  list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(
                        Html.a(ConductorTemplateComponent.getUrl(module), null, null, module.getName())
                      );
                    }
                  ));
                }
                return list;
              }
            })
            , null, null, "p-0");
        }

        ArrayList<ListenerTemplate> listenerTemplateList = ListenerTemplate.getList();
        if (listenerTemplateList.size() <= 0) {
          contents += Lte.card(null, null,
            Html.a(getUrl(KEY_ADD_LISTENER), null, null,
              Html.fasIcon("plus-square") + "Add ListenerTemplate"
            ),
            null
          );
        } else {
          contents += Lte.card("ListenerTemplate",
            Html.a(getUrl(KEY_ADD_LISTENER),
              null, null, Html.fasIcon("plus-square")
            ),
            Lte.table("table-condensed", new Lte.Table() {
              @Override
              public ArrayList<Lte.TableValue> tableHeaders() {
                ArrayList<Lte.TableValue> list = new ArrayList<>();
                //list.add(new Lte.TableValue("", "Name"));
                return list;
              }

              @Override
              public ArrayList<Future<Lte.TableRow>> tableRows() {
                ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
                for (ListenerTemplate module : listenerTemplateList) {
                  list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(
                        Html.a(ListenerTemplateComponent.getUrl(module), null, null, module.getName())
                      );
                    }
                  ));
                }
                return list;
              }
            })
            , null, null, "p-0");
        }

        return contents;
      }
    }.render(this);
  }

  private void renderAddConductorForm(ArrayList<Lte.FormError> errors) {
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
        return "Add Conductor";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(getUrl(), null, null, TITLE),
          "Add Conductor")
        );
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(KEY_ADD_CONDUCTOR), Html.Method.Post,
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

  private void renderAddListenerForm(ArrayList<Lte.FormError> errors) {
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
        return "Add Listener";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(getUrl(), null, null, TITLE),
          "Add Listener")
        );
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(KEY_ADD_LISTENER), Html.Method.Post,
            Lte.card("New ListenerTemplate", null,
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

  private void addConductorTemplate() {
    String name = request.queryParams("name");
    ConductorTemplate module = ConductorTemplate.create(name);
    response.redirect(ConductorTemplateComponent.getUrl(module));
  }


  private void addListenerTemplate() {
    String name = request.queryParams("name");
    ListenerTemplate module = ListenerTemplate.create(name);
    response.redirect(ListenerTemplateComponent.getUrl(module));
  }

  public enum Mode {Default, AddConductor, AddListener}
}
