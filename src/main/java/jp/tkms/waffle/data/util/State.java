package jp.tkms.waffle.data.util;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.data.SimulatorRun;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public enum State {
  Created, Queued, Submitted, Running, Finished, Failed, Excepted, Cancel, Canceled, None;

  public static State valueOf(int i) {
    return values()[i];
  }

  public boolean isRunning() {
    return ordinal() <= Running.ordinal();
  }

  public String getStatusBadge() {
    switch (this) {
      case Created:
        return Lte.badge("secondary", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Queued:
        return Lte.badge("info", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Submitted:
        return Lte.badge("info", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Running:
        return Lte.badge("warning", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Finished:
        return Lte.badge("success", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Failed:
        return Lte.badge("danger", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Excepted:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Cancel:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Canceled:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
      case None:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
    }
    return null;
  }
}

