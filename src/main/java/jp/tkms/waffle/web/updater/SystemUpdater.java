package jp.tkms.waffle.web.updater;

public class SystemUpdater extends AbstractUpdater {

  @Override
  public String templateBody() {
    return "";
  }

  @Override
  public String scriptArguments() {
    return "";
  }

  @Override
  public String scriptBody() {
    return "location.reload();";
  }

  public SystemUpdater() {
  }

  public SystemUpdater(Object obj) {
    super("");
  }
}
