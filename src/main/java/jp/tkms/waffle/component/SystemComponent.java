package jp.tkms.waffle.component;

import jp.tkms.waffle.Main;
import spark.Spark;

public class SystemComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  public enum Mode {Default, Hibernate}

  public SystemComponent(Mode mode) {
    this.mode = mode;
  }

  static public void register() {
    Spark.get(getUrl("hibernate"), new SystemComponent(Mode.Hibernate));
  }

  public static String getUrl(String mode) {
    return "/system/" + mode;
  }

  @Override
  public void controller() {
    logger.info(response.status() + ": " + request.url());

    switch (mode) {
      case Hibernate:
        Main.hibernate();
        break;
      default:
    }
  }
}
