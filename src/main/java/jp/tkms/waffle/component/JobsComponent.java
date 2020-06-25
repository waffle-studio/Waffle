package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.SimulatorRun;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class JobsComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  ;
  private String requestedId;
  public JobsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public JobsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(), new JobsComponent());
    Spark.get(getUrl(Mode.Cancel, null), new JobsComponent(Mode.Cancel));

    HostComponent.register();
  }

  public static String getUrl() {
    return "/jobs";
  }

  public static String getUrl(Mode mode, Job job) {
    return "/jobs/" + mode.name() + "/" + (job == null ? ":id" : job.getId());
  }

  @Override
  public void controller() {
    if (mode == Mode.Cancel) {
      Job job = Job.getInstance(request.params("id"));
      if (job != null) {
        job.cancel();
        response.redirect(getUrl());
      }
    } else {
     renderJobList();
    }
  }

  private void renderJobList() {
    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "Jobs";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          "Jobs"));
      }

      @Override
      protected String pageContent() {
        return
          Lte.card(null, null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("width:6.5em;", "ID"));
              list.add(new Lte.TableValue("", "Project"));
              list.add(new Lte.TableValue("", "Host"));
              list.add(new Lte.TableValue("width:5em;", "JobID"));
              list.add(new Lte.TableValue("width:3em;", ""));
              list.add(new Lte.TableValue("width:1em;", ""));
              return list;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (Job job : Job.getList()) {
                list.add(new Lte.TableRow(
                  Html.a(RunComponent.getUrl(job.getProject(), job.getRun()), job.getShortId()),
                  Html.a(
                    ProjectComponent.getUrl(job.getProject()),
                    job.getProject().getName()
                  ),
                  job.getHost().getName(),
                  job.getJobId(),
                  Html.spanWithId(job.getRun().getId() + "-badge", job.getRun().getState().getStatusBadge()),
                  Html.a(getUrl(Mode.Cancel, job), Html.fasIcon("times-circle"))
                  ).setAttributes(new Html.Attributes(Html.value("id", job.getRun().getId() + "-jobrow")))
                );
              }
              return list;
            }
          })
          , null, null, "p-0");
      }
    }.render(this);
  }

  public enum Mode {Default, Cancel}
}
