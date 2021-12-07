package jp.tkms.waffle.web;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.web.template.Html;
import spark.Response;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class AlertCookie {
  public static final String ALERT_ERROR = "alert-error";
  public static final String ALERT_INFO = "alert-info";

  public static String getAlertScript() {
    return Html.javascript(
      "  if(document.cookie != ''){var cookie = document.cookie.split(';');" +
        "for(var i=0;i<cookie.length;i++){" +
        "var entry = cookie[i].trim().split('=');" +
        "if('" + ALERT_ERROR + "' == entry[0]){toastr.error(decodeURIComponent(entry[1].replaceAll('\\+', ' ')));document.cookie='" + ALERT_ERROR + "=;max-age=0';}" +
        "else if('" + ALERT_INFO + "' == entry[0]){toastr.info(decodeURIComponent(entry[1].replaceAll('\\+', ' ')));document.cookie='" + ALERT_INFO + "=;max-age=0';}" +
        "}}"
      );
  }

  public static void putError(Response response, String message) {
    try {
      response.cookie("/", ALERT_ERROR, URLEncoder.encode(message, "UTF-8"), 60, false);
    } catch (UnsupportedEncodingException e) {
      ErrorLogMessage.issue(e);
    }
  }

  public static void puInfo(Response response, String message) {
    try {
      response.cookie("/", ALERT_INFO, URLEncoder.encode(message, "UTF-8"), 60, false);
    } catch (UnsupportedEncodingException e) {
      ErrorLogMessage.issue(e);
    }
  }
}
