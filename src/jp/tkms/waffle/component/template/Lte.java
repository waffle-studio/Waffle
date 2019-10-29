package jp.tkms.waffle.component.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static jp.tkms.waffle.component.template.Html.*;

public class Lte {
    public enum DivSize {F12Md6Sm3, F12Md12Sm6};

    private static String toDivSizeClass(DivSize divSize) {
        switch (divSize) {
            case F12Md6Sm3: return "col-md-3 col-sm-6 col-12";
            case F12Md12Sm6: return "col-md-6 col-sm-12 col-12";
        }

        return "col-12";
    }

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

    public static String infoBox(DivSize divSize, String icon, String iconBgCLass, String text, String number) {
        return divCol(divSize, div("info-box",
                span(listBySpace("info-box-icon", iconBgCLass), null, faIcon(icon)),
                div("info-box-content",
                        span("info-box-text", null, text),
                        span("info-box-number", null, number)
                )));
    }

    public static String formInputGroup(String type, String name, String label,
                                        String placeholder, ArrayList<FormError> errors) {
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

        return elementWithClass("table", listBySpace("table", classValue),
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
