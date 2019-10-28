package jp.tkms.waffle.component.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static jp.tkms.waffle.component.template.Html.*;

public class Lte {
    public static String card(String title, String tools, String body, String footer, String additionalClass, String additionalBodyClass) {
        String innerContent = "";
        if (title != null || tools != null) {
            innerContent =
                div("card-header",
                    h3("card-title", title),
                    div("card-tools", tools)
                );
        }
        innerContent += div("card-body" + (additionalBodyClass == null ? "" : " " + additionalBodyClass),
            removeNull(body)
        );
        if (footer != null) {
            innerContent += div("card-footer", footer);
        }
        return div("card" + (additionalClass == null ? "" : " " + additionalClass), innerContent);
    }

    public static String card(String title, String tools, String body, String footer) {
        return card(title, tools, body, footer, null, null);
    }

    public static String formInputGroup(String type, String name, String label, String placeholder, ArrayList<FormError> errors) {
        String id = "input" + name;
        return div("form-group",
            element("label", new Attributes(value("for", id)), label),
            attribute("input",
                value("type", type),
                value("class", "form-control"),
                value("name", name),
                value("id", id),
                value("placeholder", placeholder)
            )
        );
    }

    public static String formSubmitButton(String color, String value) {
        return element("button",
            new Attributes(value("type", "submit"), value("class", "btn btn-" + color)),
            value);
    }

    public static class FormError {

    }

    public static String table(String classValue, ArrayList<TableHeader> tableHeaders, ArrayList<TableRow> tableRows) {
        String headerValue = "";
        for (TableHeader header : tableHeaders) {
            headerValue += element("th", new Attributes(value("style", header.style)), header.value);
        }
        String contentsValue = "";
        for (TableRow row : tableRows) {
            String rowValue = "";
            for (String value : row) {
                rowValue += element("td", null, value);
            }
            contentsValue += element("tr", null, rowValue);
        }

        return elementWithClass("table", "table" + (classValue == null ? "" : " " + classValue),
            element("thead", null, element("tr", null , headerValue)),
            element("tbody", null, contentsValue)
        );
    }

    public static class TableHeader {
        String style;
        String value;

        public TableHeader(String style, String value) {
            this.style = style;
            this.value = value;
        }
    }

    public static class TableRow extends ArrayList<String> {
        public TableRow(String... list) {
            addAll(Arrays.asList(list));
        }
    }
}
