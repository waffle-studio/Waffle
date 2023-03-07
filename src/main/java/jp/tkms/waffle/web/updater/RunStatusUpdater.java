package jp.tkms.waffle.web.updater;

import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.data.util.State;

public class RunStatusUpdater extends AbstractUpdater {

  @Override
  public String templateBody() {
    String reloadDaemon = "";
    try {
      String caller = new Throwable().getStackTrace()[3].getClassName();
      if ("jp.tkms.waffle.component.JobsComponent".equals(caller)
        || "jp.tkms.waffle.component.TrialsComponent".equals(caller)) {
        reloadDaemon = Html.javascript("var isUpdateNeeded = false;",
          "var updateDaemon = function() {if (isUpdateNeeded) {location.reload();}};",
          "setInterval(updateDaemon, 5000);"
        );
      }
    } catch (Exception e) {}
    return Html.div(null,
      Html.divWithId("template-Created-badge",
        State.Created.getStatusBadge()
      ),
      Html.divWithId("template-Prepared-badge",
        State.Prepared.getStatusBadge()
      ),
      Html.divWithId("template-Submitted-badge",
        State.Submitted.getStatusBadge()
      ),
      Html.divWithId("template-Running-badge",
        State.Running.getStatusBadge()
      ),
      Html.divWithId("template-Finished-badge",
        State.Finished.getStatusBadge()
      ),
      Html.divWithId("template-Failed-badge",
        State.Failed.getStatusBadge()
      ),
      Html.divWithId("template-Excepted-badge",
        State.Excepted.getStatusBadge()
      ),
      Html.divWithId("template-Canceled-badge",
        State.Aborted.getStatusBadge()
      ),
      reloadDaemon
    );
  }

  @Override
  public String scriptArguments() {
    return listByComma("id", "status", "jobid", "computer", "wid", "project", "workspace", "executable");
  }

  @Override
  public String scriptBody() {
    return "try{if (status == 'Created') { isUpdateNeeded = true;" +
      "var tr = document.createElement('tr'); tr.id = id + '-jobrow'; var td = [];" +
      "for(let i=0;i<7;i+=1){td[i] = document.createElement('td'); tr.appendChild(td[i]);}" +
      "td[0].innerHTML = '<a href=\"'+id+'\">'+wid+'</a>';" +
      "td[1].innerHTML = '<a href=\"'+project+'\">'+project.replace(/.*\\//,'')+'</a>';" +
      "td[2].innerHTML = '<a href=\"'+workspace+'\">'+workspace.replace(/.*\\//,'')+'</a>';" +
      "td[3].innerHTML = '<a href=\"'+executable+'\">'+executable.replace(/.*\\//,'')+'</a>';" +
      "td[4].innerHTML = '<a href=\"'+computer+'\">'+computer.replace(/.*\\//,'')+'</a>';" +
      "td[5].id = id + '-jobid';" +
      "td[6].id = id + '-badge';" +
      "document.getElementById('jobs_table').appendChild(tr); }" +
      "else if (status == 'Submitted') { document.getElementById(id + '-jobid').innerHTML=jobid; }" +
      "else if (status == 'Failed' || status == 'Finished' || status == 'Excepted' || status == 'Canceled' || status == 'Aborted' ) { document.getElementById(id + '-jobrow').remove(); } }catch(e){}" +
      "try{document.getElementById(id + '-badge').innerHTML = document.getElementById('template-' + status + '-badge').innerHTML;}catch(e){}" +
      "try{if (id==run_id) {setTimeout(function(){location.reload();}, 1000);}}catch(e){}";
  }

  public RunStatusUpdater() {
  }

  public RunStatusUpdater(ExecutableRun run) {
    if (run.getState().equals(State.Created)) {
      Executable executable = Executable.getInstance(run.getProject(), run.getExecutable().getName());
      notify(string(run.getLocalPath().toString()), string(run.getState().toString()), string(""), string(run.getComputer().getLocalPath().toString()),
        string(run.getTaskId()), string(run.getProject().getLocalPath().toString()), string(run.getWorkspace().getLocalPath().toString()), string(executable.getLocalPath().toString()));
    } else if (run.getState().equals(State.Submitted)) {
      notify(string(run.getLocalPath().toString()), string(run.getState().toString()), string(run.getJobId()));
    } else {
      notify(string(run.getLocalPath().toString()), string(run.getState().toString()));
    }
  }
}
