package jp.tkms.waffle.web.updater;

import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.data.log.Log;

public class LogUpdater extends AbstractUpdater {

  @Override
  public String templateBody() {
    return Html.javascript(
      "var info_queue = [];" +
        "setInterval(function(){" +
        "if (info_queue.length > 0) {" +
        "try { document.getElementById('inputinfo').value = info_queue.shift(); } catch (e) {}" +
        "}" +
        "}, 250);"
    );
  }

  @Override
  public String scriptArguments() {
    return "level,timestamp,message";
  }

  @Override
  public String scriptBody() {
    return
      "try { insert_new_log(level,timestamp,message); } catch(e) {}"
        + "try { if (level === 'Warn' || level === 'Error') { toastr.error('[' + level + '] ' + message); }"
        + "else if (level === 'Debug') { toastr.info('[' + level + '] ' + message); } } catch(e) {}"
        + "info_queue.push(message);";
  }

  public LogUpdater() {
  }

  public LogUpdater(Log log) {
    super("'" + log.getLevel().name() + "'", "'" + log.getTimestamp() + "'", "'" + log.getMessage().replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'") + "'");
  }
}
