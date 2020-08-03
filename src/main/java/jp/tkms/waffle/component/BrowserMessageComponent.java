package jp.tkms.waffle.component;

import jp.tkms.waffle.data.BrowserMessage;
import jp.tkms.waffle.data.util.RubyScript;
import spark.Spark;

public class BrowserMessageComponent extends AbstractAccessControlledComponent {
  private static final String KEY_CURRENT_ROWID = "cid";

  public static void register() {
    Spark.get(getUrl(null), new BrowserMessageComponent());
  }

  public static String getUrl(String id) {
    return "/bm" + (id == null ? "/:cid" : "/" + id);
  }

  @Override
  public void controller() {

    String result = "try{rubyRunningStatus(" + (RubyScript.hasRunning() ? "true" : "false") + ");}catch(e){}";
    response.body(result);

    String browserId = request.params(KEY_CURRENT_ROWID);

    try {
      for (BrowserMessage message : BrowserMessage.getList(Long.valueOf(browserId))) {
        result += "cid=" + message.getRowId() + ";" + message.getMessage() + ";";
      }
    } catch (Exception e) {}

    response.body(result);
  }
}
