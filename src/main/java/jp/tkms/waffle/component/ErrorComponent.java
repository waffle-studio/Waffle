package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.MainTemplate;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class ErrorComponent extends AbstractAccessControlledComponent {
  static public void register() {
    Spark.notFound(new ErrorComponent());
    Spark.internalServerError(new ErrorComponent());
  }

  @Override
  public void controller() {
    logger.info(response.status() + ": " + request.url());

    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "" + response.status();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(new String[]{"" + response.status()}));
      }

      @Override
      protected String pageContent() {
        return Html.h1("text-center", Html.faIcon("question"));
      }
    }.render(this);
  }
}
