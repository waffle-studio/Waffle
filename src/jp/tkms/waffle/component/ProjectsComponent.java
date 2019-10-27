package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;

import java.util.ArrayList;
import java.util.Arrays;

public class ProjectsComponent extends AbstractComponent {
    @Override
    public void controller() {
        new MainTemplate() {
            @Override
            protected String pageTitle() {
                return "Projects";
            }

            @Override
            protected ArrayList<String> pageBreadcrumb() {
                return new ArrayList<String>(Arrays.asList(new String[]{"Projects"}));
            }

            @Override
            protected String pageContent() {
                return Lte.sampleCard();
            }
        }.render(this);
    }
}
