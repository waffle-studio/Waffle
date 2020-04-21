package jp.tkms.waffle.component;

import jp.tkms.waffle.Main;
import spark.Spark;

public class SystemComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  public enum Mode {Default, Hibernate, Restart}

  public SystemComponent(Mode mode) {
    this.mode = mode;
  }

  static public void register() {
    Spark.get(getUrl("hibernate"), new SystemComponent(Mode.Hibernate));
    Spark.get(getUrl("restart"), new SystemComponent(Mode.Restart));
  }

  public static String getUrl(String mode) {
    return "/system/" + mode;
  }

  @Override
  public void controller() {
    logger.info(response.status() + ": " + request.url());

    switch (mode) {
      case Hibernate: {
        String referer = request.headers("Referer");
        if (referer == null || "".equals(referer)) {
          referer = "/";
        }
        response.redirect(referer);
        Main.hibernate();
      }
      break;
      case Restart: {
        String referer = request.headers("Referer");
        if (referer == null || "".equals(referer)) {
          referer = "/";
        }
        response.redirect(referer);
        Main.restart();
      }
      break;
      default:
    }
  }
}
