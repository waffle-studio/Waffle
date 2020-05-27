package jp.tkms.waffle.data.util;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;

public enum HostState {
  Viable, Unviable, Stopped;

  public static HostState valueOf(int i) {
    return values()[i];
  }

  public String getStatusBadge() {
    switch (this) {
      case Viable:
        return Lte.badge("success", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Unviable:
        return Lte.badge("danger", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Stopped:
        return Lte.badge("secondary", new Html.Attributes(Html.value("style","width:6em;")), name());
    }
    return null;
  }
}
