package jp.tkms.waffle.component.template;

import static jp.tkms.waffle.component.template.Html.*;

public class Lte {
    public static String sampleCard() {
        return div("card",
                div("card-header",
                    h3("card-title", "Title"),
                    div("card-tools",
                        element("button",
                            new Attributes(
                                value("class", "btn btn-tool"),
                                value("data-card-widget", "collapse"),
                                value("data-toggle", "tooltip"),
                                value("title", "Collapse")
                            ),
                            faIcon("minus")
                        ),
                        element("button",
                            new Attributes(
                                value("class", "btn btn-tool"),
                                value("data-card-widget", "remove"),
                                value("data-toggle", "tooltip"),
                                value("title", "Remove")
                            ),
                            faIcon("times")
                        )
                    )
                ),
                div("card-body", ""),
                div("card-footer", "")
            );
    }
}
