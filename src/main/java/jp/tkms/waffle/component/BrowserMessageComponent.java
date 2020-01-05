package jp.tkms.waffle.component;

import jp.tkms.waffle.data.Browser;
import jp.tkms.waffle.data.BrowserMessage;
import spark.Spark;

public class BrowserMessageComponent extends AbstractAccessControlledComponent {
  private static final String KEY_BROWSER_ID = "bid";

  public static void register() {
    Spark.get(getUrl(null), new BrowserMessageComponent());
  }

  public static String getUrl(String id) {
    return "/bm" + (id == null ? "/:bid" : "/" + id);
  }

  @Override
  public void controller() {

    String result = "void(0);";
    response.body(result);

    String browserId = request.params(KEY_BROWSER_ID);
    Browser.update(browserId);

    for (BrowserMessage message : BrowserMessage.getList(browserId)) {
      result += message.getMessage() + ";";
      message.remove();
    }

    response.body(result);
  }
}
