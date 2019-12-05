package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.*;
import spark.QueryParamsMap;
import spark.Spark;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ConductorComponent extends AbstractComponent {
  private static final String KEY_ARGUMENTS = "arguments";
  private Mode mode;

  private Project project;
  private Conductor conductor;
  private Trial trial;
  public ConductorComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ConductorComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ConductorComponent());
    Spark.get(getUrl(null, "prepare", null), new ConductorComponent(Mode.Prepare));
    Spark.post(getUrl(null, "run", null), new ConductorComponent(Mode.Run));
    Spark.post(getUrl(null, "update-arguments", null), new ConductorComponent(Mode.UpdateArguments));

    SimulatorsComponent.register();
    TrialsComponent.register();
  }

  public static String getUrl(Conductor conductor) {
    return "/conductor/"
      + (conductor == null ? ":project/:id" : conductor.getProject().getId() + '/' + conductor.getId());
  }

  public static String getUrl(Conductor conductor, String mode, Trial trial) {
    return getUrl(conductor) + '/' + mode + '/'
      + (trial == null ? ":trial" : trial.getId());
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    if (!project.isValid()) {
    }

    conductor = Conductor.getInstance(project, request.params("id"));

    if (mode == Mode.Prepare) {
      trial = Trial.getInstance(project, request.params("trial"));
      renderPrepareForm();
    } else if (mode == Mode.Run) {
      trial = Trial.getInstance(project, request.params("trial"));
      ConductorRun run = ConductorRun.create(conductor.getProject(), trial, conductor);
      if (request.queryMap().hasKey(KEY_ARGUMENTS)) {
        run.putArguments(request.queryParams(KEY_ARGUMENTS));
      }
      run.start();
      response.redirect(JobsComponent.getUrl());
    } else if (mode == Mode.UpdateArguments) {
      if (request.queryMap().hasKey(KEY_ARGUMENTS)) {
        conductor.setArguments(request.queryParams(KEY_ARGUMENTS));
      }
      response.redirect(getUrl(conductor));
    } else {
      renderConductor();
    }
  }

  private void renderConductor() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return conductor.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          "Conductors",
          conductor.getId()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        content += Lte.card(Html.faIcon("terminal") + "Basic",
          Html.a("", Html.faIcon("edit")),
          Html.div(null,
            Html.div(null,
              "Conductor Directory",
              Lte.readonlyTextInput(conductor.getLocation().toAbsolutePath().toString())
            ),
            Html.div(null,
              "Base Script",
              Lte.readonlyTextInput(conductor.getScriptFileName())
            )
          )
          , null);

        content +=
          Html.form(getUrl(conductor, "update-arguments", trial), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "Arguments",
              Lte.cardToggleButton(true),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formTextAreaGroup(KEY_ARGUMENTS, null, 10, conductor.getArguments().toString(2), null),
                  Lte.formSubmitButton("success", "Update")
                )
              )
              , null, "collapsed-card.stop", null)
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
              for (File child : conductor.getLocation().toFile().listFiles()) {
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

  private void renderPrepareForm() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return conductor.getName();
      }

      @Override
      protected String pageSubTitle() {
        return "Prepare";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          "Conductors",
          conductor.getId()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        content +=
          Html.form(getUrl(conductor, "run", trial), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "Arguments",
              null,
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formTextAreaGroup(KEY_ARGUMENTS, null, 10, conductor.getArguments().toString(2), null),
                  Lte.formSubmitButton("primary", "Run")
                )
              )
            , null)
          );

        return content;
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  public enum Mode {Default, Prepare, Run, UpdateArguments}
}
