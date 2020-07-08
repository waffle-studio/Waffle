package jp.tkms.waffle.component.updater;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.data.SimulatorRun;
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
      Html.divWithId("template-Queued-badge",
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
        State.Canceled.getStatusBadge()
      ),
      reloadDaemon
    );
  }

  @Override
  public String scriptArguments() {
    return "id,status";
  }

  @Override
  public String scriptBody() {
    return "try{document.getElementById(id + '-badge').innerHTML = document.getElementById('template-' + status + '-badge').innerHTML;}catch(e){}" +
      "if (status == 'Created') { isUpdateNeeded = true; }" +
      "else if (status == 'Failed' || status == 'Finished' || status == 'Excepted' || status == 'Canceled' ) { try{document.getElementById(id + '-jobrow').style.display = 'none';}catch(e){} }" +
      "try{if (id==run_id) {setTimeout(function(){location.reload();}, 1000);}}catch(e){}";
  }

  public RunStatusUpdater() {
  }

  public RunStatusUpdater(SimulatorRun run) {
    super("'" + run.getId() + "'", "'" + run.getState().toString() + "'");
  }
}
