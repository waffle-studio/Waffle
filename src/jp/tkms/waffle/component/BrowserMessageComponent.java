package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Browser;
import jp.tkms.waffle.data.BrowserMessage;
import jp.tkms.waffle.data.Database;
import jp.tkms.waffle.data.Project;
import spark.Spark;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static jp.tkms.waffle.component.template.Html.*;

public class BrowserMessageComponent extends AbstractComponent {
  private static final String KEY_BROWSER_ID = "bid";

  public static void register() {
    Spark.get(getUrl(), new BrowserMessageComponent());
  }

  public static String getUrl() {
    return "/bm";
  }

  @Override
  public void controller() {
    String result = "void(0);";

    String browserId = null;

    if (request.cookies().containsKey(KEY_BROWSER_ID)) {
      browserId = request.cookie(KEY_BROWSER_ID);
      Browser.update(browserId);
    } else {
      browserId = Browser.getNewId();
      response.cookie(KEY_BROWSER_ID, browserId);
    }

    for (BrowserMessage message : BrowserMessage.getList(browserId)) {
      result += message.getMessage() + ";";
      message.remove();
    }

    response.body(result);
  }
}
