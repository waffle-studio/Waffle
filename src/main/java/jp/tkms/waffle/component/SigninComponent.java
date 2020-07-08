package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.UserSession;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class SigninComponent extends AbstractComponent {
  private Mode mode;

  ;
  public SigninComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public SigninComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(), new SigninComponent());
    Spark.post(getUrl(), new SigninComponent());
  }

  public static String getUrl() {
    return "/signin";
  }

  @Override
  public void controller() {
    if (isPost()) {
      ArrayList<Lte.FormError> errors = checkCreateProjectFormError();
      if (errors.isEmpty()) {
        getAccess(request.queryParams(UserSession.KEY_SESSION_ID));
      } else {
        renderSigninForm(errors);
      }
    } else {
      renderSigninForm(new ArrayList<>());
    }
  }

  private void renderSigninForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "Signin";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList("Signin"));
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(), Html.Method.Post,
            Lte.card(null, null,
              Html.div(null,
                Lte.formInputGroup("password", UserSession.KEY_SESSION_ID, null, "Password", null, errors)
              ),
              Lte.formSubmitButton("primary", "Verify"),
              "card-warning", null
            )
          );
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private void getAccess(String pass) {
    if (pass.equals("aisT305")) {
      UserSession session = UserSession.create();
      response.cookie("/", UserSession.KEY_SESSION_ID, session.getSessionId(), -1, false);
    }
    response.redirect(ProjectsComponent.getUrl());
  }

  public enum Mode {Default, Add}
}
