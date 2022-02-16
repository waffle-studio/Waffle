package jp.tkms.waffle.web.component;

import jp.tkms.waffle.exception.ProjectNotFoundException;
import jp.tkms.waffle.web.component.misc.HelpComponent;
import jp.tkms.waffle.web.template.Html;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

abstract public class AbstractComponent {
  protected static Logger logger = LoggerFactory.getLogger("Component");

  public Request request;
  public Response response;
  private String buffer = "";

  public static void register() {
  }

  abstract public void controller() throws ProjectNotFoundException;

  public Object handle(Request request, Response response) throws Exception {
    this.request = request;
    this.response = response;

    controller();

    return response.body();
  }

  public void setBody(String body) {
    response.body(body);
  }

  public void bufferWrite(String text) {
    buffer += text;
  }

  public String bufferFlush() {
    String result = buffer;
    buffer = "";
    return result;
  }

  public boolean isPost() {
    return request.requestMethod().toLowerCase().equals("post");
  }
}
