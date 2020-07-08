package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.ListenerTemplate;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class ListenerTemplateComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "ListenerTemplate";
  private static final String KEY_MAIN_SCRIPT = "main_script";
  private static final String KEY_ARGUMENTS = "arguments";
  private Mode mode;

  private ListenerTemplate module;
  public ListenerTemplateComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ListenerTemplateComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ListenerTemplateComponent());
    Spark.post(getUrl(null, "update-arguments"), new ListenerTemplateComponent(Mode.UpdateArguments));
    Spark.post(getUrl(null, "update-main-script"), new ListenerTemplateComponent(Mode.UpdateMainScript));
  }

  public static String getUrl(ListenerTemplate module) {
    return "/listener-template/"
      + (module == null ? ":name" : module.getName());
  }

  public static String getUrl(ListenerTemplate module, String mode) {
    return getUrl(module) + '/' + mode;
  }

  @Override
  public void controller() {
    module = ListenerTemplate.getInstance(request.params("name"));

    if (mode == Mode.UpdateArguments) {
      if (request.queryMap().hasKey(KEY_ARGUMENTS)) {
        //module.setArguments(request.queryParams(KEY_ARGUMENTS));
      }
      response.redirect(getUrl(module));
    } else if (mode == Mode.UpdateMainScript) {
      if (request.queryMap().hasKey(KEY_MAIN_SCRIPT)) {
        module.updateScript(request.queryParams(KEY_MAIN_SCRIPT));
      }
      response.redirect(getUrl(module));
    } else {
      renderConductor();
    }
  }

  private void renderConductor() {
    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return module.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(TemplatesComponent.getUrl(), TemplatesComponent.TITLE),
          "ListenerTemplate",
          module.getName()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Lte.card(Html.fasIcon("terminal") + "Properties",
          Lte.cardToggleButton(true),
          Html.div(null,
            Lte.readonlyTextInput(TITLE + " Directory", module.getDirectoryPath().toAbsolutePath().toString()),
            Lte.readonlyTextInput("Main Script", module.getScriptFileName())
          )
          , null, "collapsed-card", null);

        /*
        String argumentsText = "";
        for (String s : module.getArguments()) {
          argumentsText += s + "\n";
        }

        content +=
          Html.form(getUrl(module, "update-arguments"), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "Arguments",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formTextAreaGroup(KEY_ARGUMENTS, null, argumentsText.split("\n").length, argumentsText, null),
                  Lte.formSubmitButton("success", "Update")
                )
              )
              , null, "collapsed-card.stop", null)
          );

         */

        content +=
          Html.form(getUrl(module, "update-main-script"), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "Main Script",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formDataEditorGroup(KEY_MAIN_SCRIPT, null, "ruby", module.getScript(), errors)
                )
              )
              ,Lte.formSubmitButton("success", "Update") , "collapsed-card.stop", null)
          );

        /*
        content += Lte.card(Html.faIcon("file") + "Files",
          Lte.cardToggleButton(true),
          Lte.table("table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              return null;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (File child : module.getDirectoryPath().toFile().listFiles()) {
                list.add(new Lte.TableRow(
                  child.getName())
                );
              }
              return list;
            }
          })
          , null, "collapsed-card", "p-0");

         */

        return content;
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  public enum Mode {Default, UpdateArguments, UpdateMainScript}
}
