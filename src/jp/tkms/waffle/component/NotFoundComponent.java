package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.MainTemplate;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class NotFoundComponent extends AbstractComponent {
    static public void register() {
        Spark.notFound(new NotFoundComponent());
    }

    @Override
    public void controller() {
        logger.warn("NotFound: " + request.url());

        new MainTemplate() {
            @Override
            protected String pageTitle() {
                return "404";
            }

            @Override
            protected String pageSubTitle() {
                return "NotFound";
            }

            @Override
            protected ArrayList<String> pageBreadcrumb() {
                return new ArrayList<String>(Arrays.asList(new String[]{"NotFound"}));
            }

            @Override
            protected String pageContent() {
                return Html.h1("text-center", Html.faIcon("question"));
            }
        }.render(this);
    }
}
