package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Database;
import spark.Spark;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static jp.tkms.waffle.component.template.Html.*;

public class TestComponent extends AbstractComponent {
    static public void register() {
        Spark.get("/test", new TestComponent());
    }

    @Override
    public void controller() {
        new MainTemplate() {
            @Override
            protected String pageTitle() {
                return "Test";
            }

            @Override
            protected ArrayList<String> pageBreadcrumb() {
                return new ArrayList<String>(Arrays.asList(new String[]{"Test"}));
            }

            @Override
            protected String pageContent() {
                return sampleCard();
            }
        }.render(this);
    }

    public static String sampleCard() {


        try {
            Database db = Database.getMainDB();
            db.execute("insert into project(id,name) values('"+ UUID.randomUUID()+"','TEST');");
            db.commit();
            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }


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
