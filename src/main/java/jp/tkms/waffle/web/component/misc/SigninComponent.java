package jp.tkms.waffle.web.component.misc;

import jp.tkms.utils.crypt.DecryptingException;
import jp.tkms.utils.crypt.RSA;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.data.web.Password;
import jp.tkms.waffle.web.component.AbstractComponent;
import jp.tkms.waffle.web.component.ResponseBuilder;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.MainTemplate;
import jp.tkms.waffle.data.web.UserSession;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class SigninComponent extends AbstractComponent {
  private static final String KEY_KEYID = "keyid";
  private static final String KEY_PASSWORD = "password";
  private static final String KEY_REDIRECT = "redirect";
  private Mode mode;

  private static LinkedHashMap<Long, RSA> rsaKeyPairMap = new LinkedHashMap<>();

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

  private static String getUrl() {
    return "/signin";
  }

  public static String getUrl(String redirect) {
    return getUrl() + "?redirect=" + redirect;
  }

  @Override
  public void controller() {
    if (isPost()) {
      ArrayList<Lte.FormError> errors = checkCreateProjectFormError();
      RSA rsaKeyPair = getRsaKey(request.queryParams(KEY_KEYID));
      if (errors.isEmpty() && rsaKeyPair != null) {
        try {
          String password = rsaKeyPair.decryptToString(request.queryParams(KEY_PASSWORD));
          if (Password.isNotEmpty()) {
            getAccess(password);
            redirectToNext();
          } else {
            Password.setPassword(password);
            response.redirect(ProjectsComponent.getUrl());
          }
        } catch (DecryptingException e) {
          redirectToNext();
        }
      } else {
        renderSigninForm(errors);
      }
    } else {
      if (Password.isNotEmpty()) {
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
        Map.Entry<Long, RSA> rsaKeyEntry = getRsaKey();
        return Html.element("script", new Html.Attributes(Html.value("src", "js/jsencrypt.min.js"))) +
          Html.javascript("var pubkey='" + rsaKeyEntry.getValue().getPublicKey() + "'; var onsubmit=function(e){var p=document.getElementsByName('" + KEY_PASSWORD + "')[0];var c=new JSEncrypt();c.setPublicKey(pubkey);p.value=c.encrypt(p.value);};") +
          Html.element("form",
            new Html.Attributes(Html.value("action", getUrl()),
              Html.value("onsubmit", "onsubmit()"),
              Html.value("method", "post")
            ),
            Lte.card("Register a password to access the web interface.", null,
              Html.div(null,
                Html.inputHidden(KEY_KEYID, rsaKeyEntry.getKey().toString()),
                Lte.formInputGroup("text", KEY_PASSWORD, null, "Password", null, errors).replaceFirst("value=\"\"", "autofocus"),
                Html.br(),
                Lte.alert(Lte.Color.Secondary,
                  "You can reset your password by deleting a file \"WAFFLE/.PASSWORD\".")
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
        Map.Entry<Long, RSA> rsaKeyEntry = getRsaKey();
        return Html.element("script", new Html.Attributes(Html.value("src", "js/jsencrypt.min.js"))) +
          Html.javascript("var pubkey='" + rsaKeyEntry.getValue().getPublicKey() + "'; var onsubmit=function(e){var p=document.getElementsByName('" + KEY_PASSWORD + "')[0];var c=new JSEncrypt();c.setPublicKey(pubkey);p.value=c.encrypt(p.value);};") +
          Html.element("form",
            new Html.Attributes(Html.value("action", getUrl()),
              Html.value("onsubmit", "onsubmit()"),
              Html.value("method", "post")
            ),
            Lte.card(null, null,
              Html.div(null,
                Html.inputHidden(KEY_KEYID, rsaKeyEntry.getKey().toString()),
                Html.inputHidden(KEY_REDIRECT, request.queryParams(KEY_REDIRECT)),
                Lte.formInputGroup("password", KEY_PASSWORD, null, "Password", null, errors).replaceFirst("value=\"\"", "autofocus"),
                Html.br(),
                Lte.alert(Lte.Color.Secondary,
                  "You can reset your password by deleting a file \"WAFFLE/.PASSWORD\".")
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
    String passwordHash = Password.getPasswordHash();
    if (Password.isNotEmpty(passwordHash) && Password.authenticate(password)) {
      UserSession session = UserSession.create();
      response.cookie("/", UserSession.getWaffleId(), session.getSessionId(), -1, false);
    }
  }

  private void redirectToNext() {
    if (request.queryParams(KEY_REDIRECT) != null) {
      response.redirect(request.queryParams(KEY_REDIRECT));
    } else {
      response.redirect(ProjectsComponent.getUrl());
    }
  }

  private static Map.Entry<Long, RSA> getRsaKey() {
    Map.Entry<Long, RSA> keyEntry = Map.entry(System.currentTimeMillis(), new RSA());
    synchronized (rsaKeyPairMap) {
      ArrayList<Long> removingIdList = new ArrayList<>();
      for (Long id : rsaKeyPairMap.keySet()) {
        if (id + 60000 < System.currentTimeMillis()) {
          removingIdList.add(id);
        } else {
          break;
        }
      }
      for (Long id : removingIdList) {
        rsaKeyPairMap.remove(id);
      }
      rsaKeyPairMap.put(keyEntry.getKey(), keyEntry.getValue());
    }
    return keyEntry;
  }

  private static RSA getRsaKey(String id) {
    if (id == null) { return null; }
    synchronized (rsaKeyPairMap) {
      return rsaKeyPairMap.remove(Long.valueOf(id));
    }
  }

  public enum Mode {Default, Setup}
}
