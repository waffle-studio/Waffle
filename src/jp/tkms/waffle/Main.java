package jp.tkms.waffle;

import jp.tkms.waffle.component.*;

import java.util.Map;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");

        staticFiles.location("/static");

        NotFoundComponent.register();

        redirect.get("/", Environment.ROOT_PAGE);

        TestComponent.register();
        ProjectsComponent.register();
        ProjectComponent.register();
    }
}
