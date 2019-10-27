package jp.tkms.waffle.component;

import spark.Request;
import spark.Response;
import spark.Route;

abstract public class Component implements Route {
    public Request request;
    public Response response;

    abstract public void controller();

    @Override
    public Object handle(Request request, Response response) throws Exception {
        this.request = request;
        this.response = response;

        controller();

        return response.body();
    }

    public void bufferWrite(String text) {
        response.body(response.body() + text);
    }

    public void bufferClean() {
        response.body();
    }
}
