package jp.tkms.waffle.component.template;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.component.*;
import jp.tkms.waffle.component.updater.AbstractUpdater;
import jp.tkms.waffle.data.BrowserMessage;
import jp.tkms.waffle.data.Job;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static jp.tkms.waffle.component.template.Html.*;

abstract public class MainTemplate extends AbstractTemplate {
  @Override
  public void render(AbstractComponent component) {
    component.setBody(
      html(
        head(
          meta(value("charset", "utf-8")),
          meta(value("http-equiv", "X-UA-Compatible"),
            value("content", "IE=edge")),
          element("title", null,
            (pageTitle() == "" ? Constants.APP_NAME : pageTitle() + " | " + Constants.APP_NAME)),
          link("stylesheet", "/css/adminlte.min.css"),
          link("stylesheet", "/css/fontawesome-free.min.css"),
          link("stylesheet", "/css/ionicons.min.css"),
          link("stylesheet", "/css/gfonts.css"),
          link("stylesheet", "/css/select2.min.css"),
          link("stylesheet", "/css/icheck-bootstrap.min.css"),
          link("stylesheet", "/css/toastr.min.css"),
          link("stylesheet", "/css/custom.css"),
          element("script", new Attributes(value("src", "/js/jquery.min.js")))
        ),
        body("hold-transition",
          div("wrapper",
            elementWithClass("nav", "main-header navbar navbar-expand navbar-light",
              randerPageNavigation()
            ),
            elementWithClass("aside", "main-sidebar sidebar-light-primary elevation-4",
              a(Constants.ROOT_PAGE, "brand-link navbar-light",
                new Attributes(value("title", Constants.APP_FULL_NAME)),
                attribute("img",
                  value("src", "/img/logo.png"),
                  value("class", "brand-image"),
                  value("style", "opacity:1.0;")
                ),
                span("brand-text text-warning font-weight-bold", null, Constants.APP_NAME)
              ),
              div("sidebar",
                elementWithClass("nav", "mt-2",
                  element("ul",
                    new Attributes(
                      value("class", "nav nav-pills nav-sidebar flex-column"),
                      value("data-widget", "treeview"),
                      value("role", "menu"),
                      value("data-accordion", "false")
                    ),
                    elementWithClass("li", "nav-item",
                      a(ProjectsComponent.getUrl(), "nav-link", null,
                        faIcon("folder-open", "nav-icon"),
                        p("Projects")
                      )
                    ),
                    elementWithClass("li", "nav-item",
                      a(TemplatesComponent.getUrl(), "nav-link", null,
                        faIcon("layer-group", "nav-icon"),
                        p("Templates")
                      )
                    ),
                    elementWithClass("li", "nav-item",
                      a(JobsComponent.getUrl(), "nav-link", null,
                        faIcon("running", "nav-icon"),
                        p("Jobs", span("right badge badge-warning", new Attributes(value("id", "jobnum"))))
                      )
                    ),
                    elementWithClass("li", "nav-item",
                      a(HostsComponent.getUrl(), "nav-link", null,
                        faIcon("server", "nav-icon"),
                        p("Hosts")
                      )
                    ),
                    elementWithClass("li", "nav-header", "Status"),
                    elementWithClass("li", "nav-item",
                      Lte.disabledTextInput("info", null, "Screen reloaded"),
                      Html.javascript("var info_queue = [];" +
                        "var info = warn = error = function(m) { info_queue.push(m); };" +
                        "setInterval(function(){" +
                        "if (info_queue.length > 0) {" +
                        "document.getElementById('inputinfo').value = info_queue.shift();" +
                        "}" +
                        "}, 250);")
                    )
                    ,
                    elementWithClass("li", "nav-item",
                      a(SystemComponent.getUrl("hibernate"), "nav-link", new Attributes(value("title", String.valueOf(Main.PID))),
                        faIcon("power-off", "nav-icon"),
                        p("Hibernate")
                      )
                    )
                    /*,
                    elementWithClass("li", "nav-item",
                        a(ProjectsComponent.getUrl(), "nav-link", null,
                            faIcon("comment-alt", "nav-icon"),
                            p("Logs")
                        )
                    ),
                    elementWithClass("li", "nav-header",
                        "TEST-header"
                    ),
                    elementWithClass("li", "nav-item",
                        a("#", "nav-link", null,
                            faIcon("envelope", "nav-icon"),
                            p("TEST1")
                        )
                    )*/
                  )
                )
              )
            ),
            div("content-wrapper",
              elementWithClass("section", "content-header",
                div("container-fluid",
                  div("row mb-2",
                    div("col-sm-6",
                      h1(null,
                        pageTitle(),
                        element("small", null,
                          (pageSubTitle() != "" ? " / " + pageSubTitle() : "")
                        )
                      )
                    ),
                    div("col-sm-6",
                      randerPageBreadcrumb()
                    )
                  )
                )
              ),
              elementWithClass("section", "content",
                pageContent()
              )
            ),
            elementWithClass("footer", "main-footer", div("float-right d-none d-sm-block"),
              element("strong", null, "Copyright &copy; 2019 S.T."),
              a(SystemComponent.getUrl("update"), Lte.badge("secondary", null, "update"))
            )
          ),
          element("script", new Attributes(value("src", "/js/bootstrap.bundle.min.js"))),
          element("script", new Attributes(value("src", "/js/adminlte.min.js"))),
          element("script", new Attributes(value("src", "/js/select2.min.js"))),
          element("script", new Attributes(value("src", "/js/toastr.min.js"))),
          element("script", new Attributes(value("src", "/js/simpleimport.js"))),
          element("script", new Attributes(value("type", "text/javascript")),
            "var cid=" + BrowserMessage.getCurrentRowId() + ";" +
              "var loadBrowserMessage = function() {" +
              "simpleget('" + BrowserMessageComponent.getUrl("") + "' + cid, function(res) {try{eval(res)}catch(e){console.log(e)}setTimeout(loadBrowserMessage, 2000);})" +
              "}; " +
              "setTimeout(loadBrowserMessage, 2000);" +
              "var updateJobNum = function(n) {" +
              "if (n > 0) {" +
              "document.getElementById('jobnum').style.display = 'inline-block';" +
              "document.getElementById('jobnum').innerHTML = n;" +
              "} else {" +
              "document.getElementById('jobnum').style.display = 'none';" +
              "}" +
              "};updateJobNum(" + Job.getNum() + ");"
          ),
          element("script", new Html.Attributes( Html.value("src", "/ace/ace.js"), Html.value("type", "text/javascript")), ""),
          element("script", new Html.Attributes( Html.value("src", "/ace/ext-language_tools.js"), Html.value("type", "text/javascript")), ""),
          element("script",new Html.Attributes(Html.value("type", "text/javascript")),
            "$(function() {\n" +
              "  $('textarea[data-editor]').each(function() {\n" +
              "    var textarea = $(this);\n" +
              "    var mode = textarea.data('editor');\n" +
              "    var editDiv = $('<div>', {\n" +
              "      position: 'absolute',\n" +
              "      width: '100%',\n" +
              "      height: ((textarea.val().split('\\n').length + 3) * 1.5) + 'em',\n" +
              "      'class': textarea.attr('class')\n" +
              "    }).insertBefore(textarea);\n" +
              "    textarea.css('display', 'none');\n" +
              "    ace.require(\"ace/ext/language_tools\");\n" +
              "    var editor = ace.edit(editDiv[0]);\n" +
              "    editor.renderer.setShowGutter(true);\n" +
              "    editor.getSession().setValue(textarea.val());\n" +
              "    editor.getSession().setMode(\"ace/mode/\" + mode);\n" +
              "    editor.setTheme(\"ace/theme/textmate\");\n" +
              "    editor.setOptions({enableBasicAutocompletion: true, enableSnippets: true, enableLiveAutocompletion: true});\n" +
              "ace.config.loadModule('ace/snippets/snippets', function () {\n" +
              "        var snippetManager = ace.require('ace/snippets').snippetManager; \n" +
              "        ace.config.loadModule('ace/snippets/ruby', function(m) {\n" +
              "            if (m) { \n" +
              "                snippetManager.files.ruby = m;\n" +
              "                m.snippets = snippetManager.parseSnippetFile(m.snippetText);\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'hub.invokeListener(\"${1:listener name}\")', \n" +
              "                    tabTrigger: 'hub.invokeListener' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'hub.loadConductorTemplate(\"${1:conductor template name}\")', \n" +
              "                    tabTrigger: 'hub.loadConductorTemplate' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'hub.loadListenerTemplate(\"${1:listener template name}\")', \n" +
              "                    tabTrigger: 'hub.loadListenerTemplate' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: '${1:r} = hub.createSimulatorRun(\"${2:simulator name}\", \"${3:host name}\")', \n" +
              "                    tabTrigger: 'hub.createSimulatorRun' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: '${1:r} = hub.createConductorRun(\"${2:conductor name}\")', \n" +
              "                    tabTrigger: 'hub.createConductorRun' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'addFinalizer(\"${1:listener name}\")', \n" +
              "                    tabTrigger: 'addFinalizer' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'getResult(\"${1:key}\")', \n" +
              "                    tabTrigger: 'getResult' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'makeLocalShared(\"${1:key}\", \"${2:file}\")', \n" +
              "                    tabTrigger: 'makeLocalShared' \n" +
              "                });\n" +
              "                snippetManager.register(m.snippets, m.scope); \n" +
              "            }\n" +
              "        });\n" +
              "    });\n" +
              "    editor.resize();\n" +
              "    // copy back to textarea on form submit...\n" +
              "    textarea.closest('form').submit(function() {\n" +
              "      textarea.val(editor.getSession().getValue());\n" +
              "    })\n" +
              "  });\n" +
              "});"),
          AbstractUpdater.getUpdaterElements()
        )
      )
    );
  }

  String randerPageNavigation() {
    String innerContent = elementWithClass("li", "nav-item",
        a("#", "nav-link", new Attributes(value("data-widget", "pushmenu")),
          faIcon("bars")
        )
    );

    if (pageNavigation() != null) {
      for (Map.Entry<String, String> entry : pageNavigation()) {
        innerContent += elementWithClass("li", "nav-item",
          a(entry.getValue(), "nav-link", null, entry.getKey())
        );
      }
    }

    return elementWithClass("ul", "navbar-nav", innerContent);
  }

  String randerPageBreadcrumb() {
    String innerContent = elementWithClass("li", "breadcrumb-item",
      a(Constants.ROOT_PAGE, null, null, faIcon("home"))
    );

    int size = pageBreadcrumb().size();
    for (int i = 0; i < size; i++) {
      String crumb = pageBreadcrumb().get(i);
      innerContent += elementWithClass("li",
        "breadcrumb-item" + (i == (size - 1) ? " active" : ""), crumb);
    }

    return elementWithClass("ol", "breadcrumb float-sm-right", innerContent);
  }

  protected abstract ArrayList<Map.Entry<String, String>> pageNavigation();

  protected abstract String pageTitle();

  protected String pageSubTitle() {
    return "";
  }

  protected abstract ArrayList<String> pageBreadcrumb();

  protected abstract String pageContent();
}
