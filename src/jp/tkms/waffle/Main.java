package jp.tkms.waffle;

import jp.tkms.waffle.component.NotFoundComponent;
import jp.tkms.waffle.component.ProjectsComponent;
import jp.tkms.waffle.component.TestComponent;

import java.util.Map;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");

        staticFiles.location("/static");

        NotFoundComponent.register();

        get("/test/:test", (req, res) -> {
            for (Map.Entry<String, String> entry : req.params().entrySet()) {
                System.out.println(entry.getKey() + ":" + entry.getValue());
            }
            for (Map.Entry<String, String[]> entry : req.queryMap().toMap().entrySet()) {
                System.out.println(entry.getKey() + ":" + entry.getValue()[0]);
            }
            res.body("body");
            return "";
        });

        TestComponent.register();
        ProjectsComponent.register();
    }
}
