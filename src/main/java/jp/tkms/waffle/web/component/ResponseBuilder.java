package jp.tkms.waffle.web.component;

import spark.Request;
import spark.Response;
import spark.Route;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

public class ResponseBuilder implements Route {

  protected Supplier<AbstractComponent> supplier;

  public ResponseBuilder(Supplier<AbstractComponent> supplier) {
    this.supplier = supplier;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    return supplier.get().handle(request, response);
  }
}
