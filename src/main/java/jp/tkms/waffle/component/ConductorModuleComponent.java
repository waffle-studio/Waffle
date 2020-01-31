package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.*;
import spark.Spark;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ConductorModuleComponent extends AbstractAccessControlledComponent {
  private static final String KEY_MAIN_SCRIPT = "main_script";
  private static final String KEY_ARGUMENTS = "arguments";
  private Mode mode;

  private ConductorModule module;
  public ConductorModuleComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ConductorModuleComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ConductorModuleComponent());
    Spark.post(getUrl(null, "update-arguments"), new ConductorModuleComponent(Mode.UpdateArguments));
    Spark.post(getUrl(null, "update-main-script"), new ConductorModuleComponent(Mode.UpdateMainScript));
  }

  public static String getUrl(ConductorModule module) {
    return "/module/"
      + (module == null ? ":id" : module.getId());
  }

  public static String getUrl(ConductorModule module, String mode) {
    return getUrl(module) + '/' + mode;
  }

  @Override
  public void controller() {
    module = ConductorModule.getInstance(request.params("id"));

    if (mode == Mode.UpdateArguments) {
      if (request.queryMap().hasKey(KEY_ARGUMENTS)) {
        module.setArguments(request.queryParams(KEY_ARGUMENTS));
      }
      response.redirect(getUrl(module));
    } else if (mode == Mode.UpdateMainScript) {
      if (request.queryMap().hasKey(KEY_MAIN_SCRIPT)) {
        module.updateMainScriptContents(request.queryParams(KEY_MAIN_SCRIPT));
      }
      response.redirect(getUrl(module));
    } else {
      renderConductor();
    }
  }

  private void renderConductor() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return module.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          "ConductorModules",
          module.getId()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Lte.card(Html.faIcon("terminal") + "Basic", null,
          Html.div(null,
            Lte.readonlyTextInput("ConductorModule Directory", module.getLocation().toAbsolutePath().toString()),
            Lte.readonlyTextInput("Main Script", module.getScriptFileName())
          )
          , null);

        content +=
          Html.form(getUrl(module, "update-main-script"), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "Main Script",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formDataEditorGroup(KEY_MAIN_SCRIPT, null, "ruby", module.getMainScriptContents(), errors),
                  Lte.formSubmitButton("success", "Update")
                )
              )
              , null, "collapsed-card.stop", null)
          );

        content +=
          Html.form(getUrl(module, "update-arguments"), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "Arguments",
              Lte.cardToggleButton(true),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formTextAreaGroup(KEY_ARGUMENTS, null, 15, module.getArguments().toString(2), null),
                  Lte.formSubmitButton("success", "Update")
                )
              )
              , null, "collapsed-card", null)
          );

        content += Lte.card(Html.faIcon("file") + "Files", null,
          Lte.table("table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              return null;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (File child : module.getLocation().toFile().listFiles()) {
                list.add(new Lte.TableRow(
                  child.getName())
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

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  public enum Mode {Default, UpdateArguments, UpdateMainScript}
}
