package jp.tkms.waffle.web.component;

import jp.tkms.waffle.data.web.UserSession;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import jp.tkms.waffle.web.component.misc.SigninComponent;
import spark.Request;
import spark.Response;

abstract public class AbstractAccessControlledComponent extends AbstractComponent {

  @Override
  public Object handle(Request request, Response response) throws Exception {
    this.request = request;
    this.response = response;

    String sessionId = request.cookie(UserSession.getWaffleId());
    sessionId = (sessionId == null ? "" : sessionId);

    try {
      if (UserSession.isContains(sessionId)) {
        controller();
      } else {
        response.redirect(SigninComponent.getUrl(request.url()));
      }
    } catch (ProjectNotFoundException e) {

    }


    return response.body();
  }
}
