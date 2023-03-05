package jp.tkms.waffle.data.util;

import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;

import java.util.ArrayList;

public enum State {
  Created, Prepared, Submitted, Running, Finalizing, Finished, Failed, Excepted, Abort, Aborted, Cancel, Canceled, Retrying, None;

  static final ArrayList<State> displayPriority = new ArrayList<>() {
    {
      add(Running);
      add(Retrying);
      add(Created);
      add(Prepared);
      add(Submitted);
      add(Finalizing);
      add(Cancel);
      add(Abort);
      add(Excepted);
      add(Failed);
      add(Canceled);
      add(Aborted);
      add(Finished);
      add(None);
    }
  };

  public static State valueOf(int i) {
    return values()[i];
  }

  public boolean isRunning() {
    return ordinal() < Finished.ordinal();
  }

  public String getStatusBadge() {
    switch (this) {
      case Created:
        return Lte.badge("secondary", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Prepared:
        return Lte.badge("info", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Submitted:
        return Lte.badge("info", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Running:
        return Lte.badge("warning", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Finished:
        return Lte.badge("success", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Finalizing:
        return Lte.badge("warning", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Failed:
        return Lte.badge("danger", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Excepted:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Abort:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Aborted:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Cancel:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Canceled:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
      case Retrying:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
      case None:
        return Lte.badge("dark", new Html.Attributes(Html.value("style","width:6em;")), name());
    }
    return null;
  }

  public State getHigh(State state) {
    if (displayPriority.indexOf(this) < displayPriority.indexOf(state)) {
      return this;
    } else {
      return state;
    }
  }
}

