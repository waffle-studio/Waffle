package jp.tkms.waffle.web.updater;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
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
      "Object.keys(jsonobject).forEach((key)=>{" +
      "var value = jsonobject[key];" +
      "document.getElementsByName('" + NAME_PREFIX + "' + key).forEach((node)=>{" +
      "if(node.hasAttribute('value')){node.value=value;var event = document.createEvent('HTMLEvents');event.initEvent('change', true, true);node.dispatchEvent(event);}else{node.innerHTML=value;}" +
      "});" +
      "});" +
      "}catch(e){console.log(e);}";
  }

  public GeneralUpdater() {
  }

  public GeneralUpdater(JsonObject jsonObject) {
    super(createMessageValue(jsonObject));
  }

  static String createMessageValue(JsonObject jsonObject) {
    return ("'" + Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8)) + "'");
  }

  public static String generateHtml(String name, BiFunction<String, String, String> innerHtml) {
    return innerHtml.apply(NAME_PREFIX + name, DataAgent.request(name).get(name).toString()) +
      Html.javascript("bmSubscribe('" + name + "');");
  }

  public static String getUpdateScriptDirectly(String request) {
    String script = "";
    if (request != null && !"".equals(request)) {
      JsonObject jsonObject = new JsonObject();
      for (String name : request.split(",")) {
        jsonObject.add(name, DataAgent.request(name).get(name));
      }
      script = createUpdateScript(GeneralUpdater.class, createMessageValue(jsonObject));
    }
    return script;
  }
}
