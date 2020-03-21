package jp.tkms.waffle.component.updater;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.data.SimulatorRun;

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
        Lte.badge("secondary", new Html.Attributes(Html.value("style","width:6em;")), "Created")
      ),
      Html.divWithId("template-Queued-badge",
        Lte.badge("info", new Html.Attributes(Html.value("style","width:6em;")), "Queued")
      ),
      Html.divWithId("template-Submitted-badge",
        Lte.badge("info", new Html.Attributes(Html.value("style","width:6em;")), "Submitted")
      ),
      Html.divWithId("template-Running-badge",
        Lte.badge("warning", new Html.Attributes(Html.value("style","width:6em;")), "Running")
      ),
      Html.divWithId("template-Finished-badge",
        Lte.badge("success", new Html.Attributes(Html.value("style","width:6em;")), "Finished")
      ),
      Html.divWithId("template-Failed-badge",
        Lte.badge("danger", new Html.Attributes(Html.value("style","width:6em;")), "Failed")
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
      "else if (status == 'Failed' || status == 'Finished') { try{document.getElementById(id + '-jobrow').style.display = 'none';}catch(e){} }" +
      "try{if (id==run_id) {setTimeout(function(){location.reload();}, 1000);}}catch(e){}";
  }

  public RunStatusUpdater() {
  }

  public RunStatusUpdater(SimulatorRun run) {
    super("'" + run.getId() + "'", "'" + run.getState().toString() + "'");
  }
}
