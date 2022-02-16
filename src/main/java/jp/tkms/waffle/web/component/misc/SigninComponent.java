package jp.tkms.waffle.web.component.misc;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.web.component.AbstractComponent;
import jp.tkms.waffle.web.component.ResponseBuilder;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.project.executable.ExecutablesComponent;
import jp.tkms.waffle.web.component.project.workspace.WorkspaceComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.MainTemplate;
import jp.tkms.waffle.data.web.UserSession;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import spark.Spark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class SigninComponent extends AbstractComponent {
  private static final String KEY_PASSWORD = "password";
  private Mode mode;

  public SigninComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public SigninComponent() {
    this(Mode.Default);
  }

  public static void register() {
    Spark.get(getUrl(), new ResponseBuilder(() -> new SigninComponent()));
    Spark.post(getUrl(), new ResponseBuilder(() -> new SigninComponent()));
  }

  public static String getUrl() {
    return "/signin";
  }

  @Override
  public void controller() {
    if (isPost()) {
      ArrayList<Lte.FormError> errors = checkCreateProjectFormError();
      if (errors.isEmpty()) {
        if (isNotPasswordEmpty()) {
          getAccess(request.queryParams(KEY_PASSWORD));
        } else {
          setPassword(request.queryParams(KEY_PASSWORD));
        }
      } else {
        renderSigninForm(errors);
      }
    } else {
      if (isNotPasswordEmpty()) {
        renderSigninForm(new ArrayList<>());
      } else {
        renderSetupForm(new ArrayList<>());
      }
    }
  }

  private void renderSetupForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected String renderPageSidebar() {
        return "";
      }

      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected ArrayList<String> pageRightNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "Password Setup";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList());
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(), Html.Method.Post,
            Lte.card("Register a password to access the web interface.", null,
              Html.div(null,
                Lte.formInputGroup("text", KEY_PASSWORD, null, "Password", null, errors),
                Html.br(),
                Lte.alert(Lte.Color.Danger,
                  "Do not register a text that already used in other services, because the password will be saved as a plain text file on \".PASSWORD\". " +
                    "In other words, you can get the registered password and change it by the file.")
              ),
              Lte.formSubmitButton("success", "Register"),
              "card-secondary", null
            )
          );
      }
    }.render(this);
  }

  private void renderSigninForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected String renderPageSidebar() {
        return "";
      }

      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected ArrayList<String> pageRightNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "Signin";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList());
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(), Html.Method.Post,
            Lte.card(null, null,
              Html.div(null,
                Lte.formInputGroup("password", KEY_PASSWORD, null, "Password", null, errors),
                Html.br(),
                Lte.alert(Lte.Color.Secondary,
                    "You can get the registered password and change it from \".PASSWORD\".")
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

  private void getAccess(String password) {
    String actualPassword = getPassword();
    if (isNotPasswordEmpty(actualPassword) && password.equals(actualPassword)) {
      UserSession session = UserSession.create();
      response.cookie("/", UserSession.KEY_SESSION_ID, session.getSessionId(), -1, false);
    }
    response.redirect(ProjectsComponent.getUrl());
  }

  private void setPassword(String password) {
    if (isNotPasswordEmpty(password)) {
      try {
        Path passwordFilePath = getPasswordFilePath();
        Files.writeString(passwordFilePath, password);
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }
    response.redirect(ProjectsComponent.getUrl());
  }

  private static boolean isNotPasswordEmpty() {
    return isNotPasswordEmpty(getPassword());
  }

  private static boolean isNotPasswordEmpty(String password) {
    return !"".equals(password);
  }

  private static String getPassword() {
    Path passwordFilePath = getPasswordFilePath();
    if (!passwordFilePath.toFile().exists()) {
      try {
        Files.writeString(passwordFilePath, "");
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    String password = null;
    try {
      password = Files.readString(Constants.WORK_DIR.resolve(Constants.PASSWORD)).trim();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    return password;
  }

  private static Path getPasswordFilePath() {
    return Constants.WORK_DIR.resolve(Constants.PASSWORD);
  }

  public enum Mode {Default, Setup}
}
