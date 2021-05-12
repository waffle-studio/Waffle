package jp.tkms.waffle.web.updater;

import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.data.DataAgent;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.web.template.Html;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.BiFunction;
import java.util.function.Function;

public class GeneralUpdater extends AbstractUpdater {
  public static final String NAME_PREFIX = "W.";

  @Override
  public String templateBody() {
    return Html.javascript("bm_subscribed = new Set();" +
      "bmSubscribe = function(name) { bm_subscribed.add(name); };");
  }

  @Override
  public String scriptArguments() {
    return "b64jsonobject";
  }

  @Override
  public String scriptBody() {
    return "try{var jsonobject = JSON.parse(decodeURIComponent(escape(window.atob(b64jsonobject))));" +
      "jsonobject.forEach((value,key)=>{" +
      "document.getElementsByName('" + NAME_PREFIX + "' + key).forEach((node)=>{" +
      "if(node.hasAttribute('value')){node.value=value;}else{node.innerHTML=value;}" +
      "});" +
      "});" +
      "}catch(e){}";
  }

  public GeneralUpdater() {
  }

  public GeneralUpdater(JsonObject jsonObject) {
    super("'" + Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8)) + "'");
  }

  public String getHtml(String name, BiFunction<String, String, String> innerHtml) {
    return innerHtml.apply(NAME_PREFIX + name, DataAgent.request(name).getString(name, "")) +
      Html.javascript("bmSubscribe('" + name + "');");
  }
}
