package jp.tkms.waffle.component.updater;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.data.Run;

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
    super();
  }
}
