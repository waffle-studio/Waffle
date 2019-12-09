package jp.tkms.waffle.component.template;

import java.util.ArrayList;
import java.util.Arrays;

import static jp.tkms.waffle.component.template.Html.*;

public class Lte {
  private static String toDivSizeClass(DivSize divSize) {
    switch (divSize) {
      case F12Md6Sm3:
        return "col-md-3 col-sm-6 col-12";
      case F12Md12Sm6:
        return "col-md-6 col-sm-12 col-12";
    }

    return "col-12";
  }

  ;

  public static String divContainerFluid(String... values) {
    return div("container-fluid", values);
  }

  public static String divRow(String... values) {
    return div("row", values);
  }

  public static String divCol(DivSize divSize, String... values) {
    return div(toDivSizeClass(divSize), values);
  }

  public static String card(String title, String tools, String body, String footer, String additionalClass,
                            String additionalBodyClass) {
    String innerContent = "";
    if (title != null || tools != null) {
      innerContent =
        div("card-header",
          h3("card-title", title),
          div("card-tools", tools)
        );
    }
    innerContent += div(listBySpace("card-body", additionalBodyClass),
      removeNull(body)
    );
    if (footer != null) {
      innerContent += div("card-footer", footer);
    }
    return div(listBySpace("card", additionalClass), innerContent);
  }

  public static String card(String title, String tools, String body, String footer) {
    return card(title, tools, body, footer, null, null);
  }

  public static String badge(String colorType, Attributes attributes, String value) {
    return Html.span("badge badge-" + colorType, attributes, value);
  }

  public static String cardToggleButton(boolean flag) {
    return Html.element("button", new Attributes(value("class", "btn btn-tool"),
      value("type", "button"), value("data-card-widget","collapse"),
      value("data-toggle", "tooltip")), (flag?Html.faIcon("plus"):Html.faIcon("minus")));
  }

  public static String infoBox(DivSize divSize, String icon, String iconBgCLass, String text, String number) {
    return divCol(divSize, div("info-box",
      span(listBySpace("info-box-icon", iconBgCLass), null, faIcon(icon)),
      div("info-box-content",
        span("info-box-text", null, text),
        span("info-box-number", null, number)
      )));
  }

  public static String formTextAreaGroup(String name, String label, int rows,
                                      String contents, ArrayList<FormError> errors) {
    String id = "input" + name;
    return div("form-group",
      (label != null ?
        element("label", new Attributes(value("for", id)), label) : null),
      element("textarea",
        new Attributes(
            value("class", "form-control"),
            value("rows", String.valueOf(rows)),
            value("name", name),
            value("id", id)
        )
        , contents
      )
    );
  }

  public static String readonlyTextAreaGroup(String name, String label, int rows,
                                         String contents) {
    String id = "input" + name;
    return div("form-group",
      (label != null ?
        element("label", new Attributes(value("for", id)), label) : null),
      element("textarea",
        new Attributes(
          value("class", "form-control"),
          value("rows", String.valueOf(rows)),
          value("name", name),
          value("id", id),
          value("readonly", null)
        )
        , contents
      )
    );
  }

  public static String formInputGroup(String type, String name, String label,
                                      String placeholder, ArrayList<FormError> errors) {
    String id = "input" + name;
    return div("form-group",
      (label != null ?
        element("label", new Attributes(value("for", id)), label) : null),
      attribute("input",
        value("type", type),
        value("class", "form-control"),
        value("name", name),
        value("id", id),
        value("placeholder", placeholder)
      )
    );
  }

  public static String formSelectGroup(String name, String label, ArrayList<String> optionList, ArrayList<FormError> errors) {
    String id = "input" + name;
    String options = "";
    for (String option : optionList) {
      options += element("option", null, option);
    }
    return div("form-group",
      (label != null ? element("label", new Attributes(value("for", id)), label) : null),
      element("select", new Attributes(value("name", name), value("id", id), value("class", "form-control")), options)
    );
  }

  public static String formSelect2Group(String name, String label, ArrayList<String> optionList, ArrayList<FormError> errors) {
    String id = "input" + name;
    String options = "";
    for (String option : optionList) {
      options += element("option", null, option);
    }
    return div("form-group",
      (label != null ? element("label", new Attributes(value("for", id)), label) : null),
      element("select", new Attributes(value("id", id),value("style", "height:1.5em;"),
        value("class", "form-control select2")), options),
      Html.javascript("$(document).ready(function(){$('#" + id + "').select2()});")
    );
  }

  public static String formSubmitButton(String color, String value) {
    return element("button",
      new Attributes(value("type", "submit"), value("class", "btn btn-" + color)),
      value);
  }

  public static String disabledTextInput(String value) {
    return div("form-group",
      attribute("input",
        value("type", "text"),
        value("value", value),
        value("class", "form-control"),
        value("disabled", null)
      )
    );
  }

  public static String readonlyTextInput(String value) {
    return div("form-group",
      attribute("input",
        value("type", "text"),
        value("value", value),
        value("class", "form-control"),
        value("readonly", null)
      )
    );
  }

  public static String table(String classValue, Table table) {
    String headerValue = "";
    if (table.tableHeaders() != null) {
      for (TableValue header : table.tableHeaders()) {
        headerValue += element("th", new Attributes(value("style", header.style)), header.value);
      }
    }
    String contentsValue = "";
    for (TableRow row : table.tableRows()) {
      String rowValue = "";
      for (TableValue value : row) {
        rowValue += element("td", new Attributes(value("style", value.style)), value.value);
      }
      contentsValue += element("tr", null, rowValue);
    }

    return elementWithClass("table", listBySpace("table", classValue),
      (table.tableHeaders() != null ?
        element("thead", null, element("tr", null, headerValue)) : null),
      element("tbody", null, contentsValue)
    );
  }

  public enum DivSize {F12, F12Md6Sm3, F12Md12Sm6}

  public static class FormError {

  }

  public static class TableValue {
    String style;
    String value;

    public TableValue(String style, String value) {
      this.style = style;
      this.value = value;
    }
  }

  public static class TableRow extends ArrayList<TableValue> {
    Html.Attributes attributes = null;

    public TableRow(TableValue... list) {
      super(Arrays.asList(list));
    }

    public TableRow(String... list) {
      for (String value : list) {
        add(new TableValue(null, value));
      }
    }
  }

  public abstract static class Table {
    public abstract ArrayList<TableValue> tableHeaders();
    public abstract ArrayList<TableRow> tableRows();
  }
}
