package jp.tkms.waffle.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

abstract public class AbstractComponent implements Route {
    static Logger logger = LoggerFactory.getLogger("Component");

    public Request request;
    public Response response;

    public static void register() {};

    public static String getUrl(String... values) {
        return "/";
    }

    abstract public void controller();

    @Override
    public Object handle(Request request, Response response) throws Exception {
        this.request = request;
        this.response = response;

        controller();

        return response.body();
    }

    public void setBody(String body) {
        response.body(body);
    }

    private String buffer = "";

    public void bufferWrite(String text) {
        buffer += text;
    }

    public String bufferFlush() {
        String result = buffer;
        buffer = "";
        return result;
    }
}