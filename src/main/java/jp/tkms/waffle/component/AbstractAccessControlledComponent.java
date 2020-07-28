package jp.tkms.waffle.component;

import jp.tkms.waffle.PollingThread;
import jp.tkms.waffle.data.UserSession;
import jp.tkms.waffle.data.exception.ProjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

abstract public class AbstractAccessControlledComponent extends AbstractComponent {

  @Override
  public Object handle(Request request, Response response) throws Exception {
    this.request = request;
    this.response = response;

    String sessionId = request.cookie(UserSession.KEY_SESSION_ID);
    sessionId = (sessionId == null ? "" : sessionId);

    try {
      if (UserSession.isContains(sessionId)) {
        controller();
      } else {
        response.redirect(SigninComponent.getUrl());
      }
    } catch (ProjectNotFoundException e) {

    }


    return response.body();
  }
}
