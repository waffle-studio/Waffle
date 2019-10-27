package jp.tkms.waffle.component;

public class Test extends AbstractComponent {
    @Override
    public void controller() {
        response.body("OK");
    }
}
