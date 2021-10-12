package jp.tkms.waffle.web.template;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.web.component.*;
import jp.tkms.waffle.web.component.job.JobsComponent;
import jp.tkms.waffle.web.component.log.LogsComponent;
import jp.tkms.waffle.web.component.misc.BrowserMessageComponent;
import jp.tkms.waffle.web.component.misc.SystemComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.computer.ComputersComponent;
import jp.tkms.waffle.web.updater.AbstractUpdater;
import jp.tkms.waffle.data.web.BrowserMessage;
import jp.tkms.waffle.data.internal.task.ExecutableRunTask;
import jp.tkms.waffle.script.ruby.util.RubyScript;

import java.util.ArrayList;
import java.util.Map;

import static jp.tkms.waffle.web.template.Html.*;

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
            (pageTitle() == "" ? Constants.APP_NAME : pageTitle()
              + (pageSubTitle() == "" ? "" : "/" + pageSubTitle())
              + " | " + Constants.APP_NAME)),
          link("stylesheet", "/css/adminlte.min.css"),
          link("stylesheet", "/css/fontawesome-free.min.css"),
          link("stylesheet", "/css/ionicons.min.css"),
          link("stylesheet", "/css/gfonts.css"),
          link("stylesheet", "/css/select2.min.css"),
          link("stylesheet", "/css/icheck-bootstrap.min.css"),
          link("stylesheet", "/css/toastr.min.css"),
          link("stylesheet", "/css/custom.css"),
          link("stylesheet", "/jsoneditor/jsoneditor.min.css"),
          element("script", new Attributes(value("src", "/jsoneditor/jsoneditor.min.js"))),
          element("script", new Attributes(value("src", "/js/jquery.min.js")))
        ),
        body("hold-transition layout-footer-fixed layout-fixed",
          AbstractUpdater.getUpdaterElements(),
          div("wrapper",
            elementWithClass("nav", "main-header navbar navbar-expand navbar-light",
              renderPageNavigation(),
              renderPageRightNavigation()
            ),
            elementWithClass("aside", "main-sidebar sidebar-light-primary elevation-4",
              a(Constants.ROOT_PAGE, "brand-link navbar-light",
                new Attributes(value("title", Constants.APP_NAME_MEANING)),
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
                    renderPageSidebar()
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
                        (enableParentLink() ?
                        element("small", null,
                          a("javascript:gotoParent();", span(null, new Attributes(value("style", "color:#ffc107;")), fasIcon("caret-square-up")))
                        ) : ""),
                        pageTitle(),
                        element("small", null,
                          (pageSubTitle() != "" ? " / " + pageSubTitle() : "")
                        )
                      )
                    ),
                    div("col-sm-6",
                      renderPageBreadcrumb()
                    )
                  ),
                  div(null, pageTool())
                )
              ),
              elementWithClass("section", "content",
                pageContent()
              )
            ),
            elementWithClass("footer", "main-footer", div("float-right d-none d-sm-block"),
              Lte.disabledTextInput("info", null, "Screen reloaded (WAFFLE:" + Main.VERSION + ")")
              /*
              element("strong", null, "Copyright &copy; 2019 Waffle Developer Team"),
              a(SystemComponent.getUrl(SystemComponent.Mode.Update), Lte.badge("secondary", null, "update (" + Main.VERSION + ")"))
               */
            )
          ),
          element("script", new Attributes(value("src", "/js/bootstrap.bundle.min.js"))),
          element("script", new Attributes(value("src", "/js/adminlte.min.js"))),
          element("script", new Attributes(value("src", "/js/select2.min.js"))),
          element("script", new Attributes(value("src", "/js/toastr.min.js"))),
          element("script", new Attributes(value("src", "/js/jquery.knob.min.js"))),
          element("script", new Attributes(value("type", "text/javascript")),
              "$(function(){$('.dial').knob();});"
          ),
          element("script", new Attributes(value("src", "/js/simpleimport.js"))),
          element("script", new Attributes(value("type", "text/javascript")),
            "var gotoParent = function() {" +
              "window.location.href = window.location.href.replace(/^(.*)\\/.*$/,'$1');" +
              "};" +
              "var cid=" + BrowserMessage.getCurrentRowId() + ";" +
              "var loadBrowserMessage = function() {" +
              "var r = ''; if(undefined != bm_subscribed){r = Array.from(bm_subscribed).join(',');}" +
              "simplepost('" + BrowserMessageComponent.getUrl("") + "' + cid, r, function(res) {try{eval(res)}catch(e){console.log(e)}setTimeout(loadBrowserMessage, 2000);})" +
              "}; " +
              "setTimeout(loadBrowserMessage, 5000);" +
              "var updateJobNum = function(n) {" +
              "if (n > 0) {" +
              "document.getElementById('jobnum').style.display = 'inline-block';" +
              "document.getElementById('jobnum').innerHTML = n;" +
              "} else {" +
              "document.getElementById('jobnum').style.display = 'none';" +
              "}" +
              "};updateJobNum(" + ExecutableRunTask.getNum() + ");" +
              "if (sessionStorage.getItem('latest-project-id') != null) {" +
              "document.getElementById('recently-accessed-project').innerHTML=\"<a class='nav-link' title='recently accessed' href='/PROJECT/\"+sessionStorage.getItem('latest-project-id')+\"'><i class='nav-icon fas fa-angle-right' style='margin-lefti:1rem;'></i><p>\"+sessionStorage.getItem('latest-project-name')+\"</p></a>\";" +
              "} else {document.getElementById('recently-accessed-project').style.display='none';}"
          ),
          element("script", new Html.Attributes( Html.value("src", "/ace/ace.js"), Html.value("type", "text/javascript")), ""),
          element("script", new Html.Attributes( Html.value("src", "/ace/ext-language_tools.js"), Html.value("type", "text/javascript")), ""),
          element("script",new Html.Attributes(Html.value("type", "text/javascript")),
            "$(function() {\n" +
              "  $('textarea[data-editor]').each(function() {\n" +
              "    var textarea = $(this);\n" +
              "    var mode = textarea.data('editor');\n" +
              "    var snippetScript = textarea.data('snippet');\n" +
              "    var editDiv = $('<div>', {\n" +
              "      position: 'absolute',\n" +
              "      width: '100%',\n" +
              "      'class': textarea.attr('class')\n" +
              "    }).insertBefore(textarea);\n" +
              "    textarea.css('display', 'none');\n" +
              "    ace.require(\"ace/ext/language_tools\");\n" +
              "    var editor = ace.edit(editDiv[0]);\n" +
              "    editor.renderer.setShowGutter(true);\n" +
              "    editor.getSession().setValue(textarea.val());\n" +
              "    editor.getSession().setMode(\"ace/mode/\" + mode);\n" +
              "    editor.setTheme(\"ace/theme/textmate\");\n" +
              "    editor.setOptions({enableBasicAutocompletion: true, enableSnippets: true, enableLiveAutocompletion: true, maxLines: Infinity});\n" +
              "    ace.config.loadModule('ace/snippets/snippets', function () {\n" +
              "        var snippetManager = ace.require('ace/snippets').snippetManager; \n" +
              "        ace.config.loadModule('ace/snippets/' + mode, function(m) {\n" +
              "            if (m) { \n" +
              "                snippetManager.files.ruby = m;\n" +
              "                m.snippets = snippetManager.parseSnippetFile(m.snippetText);\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'this.invokeListener(\"${1:listener name}\")', \n" +
              "                    tabTrigger: 'this.invokeListener' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'this.loadConductorTemplate(\"${1:conductor template name}\")', \n" +
              "                    tabTrigger: 'this.loadConductorTemplate' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'this.loadListenerTemplate(\"${1:listener template name}\")', \n" +
              "                    tabTrigger: 'this.loadListenerTemplate' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'this.createExecutableRun(\"${1:executable name}\", \"${2:computer name}\")', \n" +
              "                    tabTrigger: 'this.createExecutableRun' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'this.createConductorRun(\"${1:conductor name}\")', \n" +
              "                    tabTrigger: 'this.createConductorRun' \n" +
              "                });\n" +
              "                m.snippets.push({ \n" +
              "                    content: 'addFinalizer(\"${1:procedure name}\")', \n" +
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
              "                var s = function(trigger, content) {\n" +
              "                    m.snippets.push({ \n" +
              "                        content: content, \n" +
              "                        tabTrigger: trigger \n" +
              "                    });\n" +
              "                };\n" +
              "                if (snippetScript != \"\") { eval(snippetScript); }" +
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
          element("script",new Html.Attributes(Html.value("type", "text/javascript")),
            "$(function() {\n" +
              "  $('textarea[data-jsoneditor]').each(function() {\n" +
              "    var textarea = $(this);\n" +
              "    var mode = textarea.data('jsoneditor');\n" +
              "    var textmode = (mode=='view'?'preview':'code');\n" +
              "    var editDiv = $('<div>', {\n" +
              "      width: '100%',\n" +
              "    }).insertBefore(textarea);\n" +
              "    textarea.css('display', 'none');\n" +
              "    var editor = new JSONEditor(editDiv[0], {\"onModeChange\":function(mode){if(mode=='code'){editor.aceEditor.setOptions({maxLines: Infinity})}},\"ace\":ace,\"language\":\"en\",\"statusBar\":false," +
              "\"navigationBar\":false,\"enableTransform\":false,\"enableSort\":false,\"search\": false," +
              "\"autocomplete\":{getOptions:function(){return [];}}," +
              "\"modes\":[mode,textmode]}, JSON.parse(textarea.val()));\n" +
              "    //editor.editor.setOptions({maxLines: Infinity});\n" +
              "    // copy back to textarea on form submit...\n" +
              "    textarea.closest('form').submit(function() {\n" +
              "      textarea.val(editor.getText());\n" +
              "    })\n" +
              "  });\n" +
              "});")
        )
      )
    );
  }

  protected String renderPageSidebar() {
    return
      elementWithClass("li", "nav-item",
        a(ProjectsComponent.getUrl(), "nav-link", null,
          fasIcon("folder-open", "nav-icon"),
          p("Projects")
        )
      ) +
      element("li",
        new Attributes(
          value("class", "nav-item"),
          value("style", "padding-left:1rem;"),
          value("id", "recently-accessed-project")
        ),
        a("", "nav-link", null,
          p("null")
        )
      ) +
        /*
      elementWithClass("li", "nav-item",
        a(TemplatesComponent.getUrl(), "nav-link", null,
          fasIcon("layer-group", "nav-icon"),
          p("Templates")
        )
      ) +
         */
      elementWithClass("li", "nav-item",
        a(JobsComponent.getUrl(), "nav-link", null,
          fasIcon("running", "nav-icon"),
          p("Jobs", span("right badge badge-warning", new Attributes(value("id", "jobnum"))))
        )
      ) +
      elementWithClass("li", "nav-item",
        a(ComputersComponent.getUrl(), "nav-link", null,
          fasIcon("server", "nav-icon"),
          p("Computers")
        )
      ) +
      elementWithClass("li", "nav-item",
        a(LogsComponent.getUrl(), "nav-link", null,
          fasIcon("quote-left", "nav-icon"),
          p("Logs")
        )
      ) +
      elementWithClass("li", "nav-item",
        a(SystemComponent.getUrl(), "nav-link", null,
          fasIcon("cog", "nav-icon"),
          p("System")
        )
      );
        /*+
      elementWithClass("li", "nav-header", "Status") +
      elementWithClass("li", "nav-item",
        Lte.disabledTextInput("info", null, "Screen reloaded")
      ) +
      elementWithClass("li", "nav-item",
        a(SystemComponent.getUrl(SystemComponent.Mode.Hibernate), "nav-link", new Attributes(value("title", String.valueOf(Main.PID))),
          fasIcon("power-off", "nav-icon"),
          p("Hibernate")
        )
      );
         */
  }

  String renderPageNavigation() {
    String innerContent = elementWithClass("li", "nav-item",
      a("#", "nav-link", new Attributes(value("data-widget", "pushmenu")),
        fasIcon("bars")
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

  String renderPageRightNavigation() {
    String innerContent = "";

    if (pageRightNavigation() != null) {
      for (String value : pageRightNavigation()) {
        innerContent += elementWithClass("li", "nav-item", value);
      }
    }

    return elementWithClass("ul", "navbar-nav ml-auto", innerContent);
  }

  String renderPageBreadcrumb() {
    String innerContent = elementWithClass("li", "breadcrumb-item",
      a(Constants.ROOT_PAGE, null, null, fasIcon("home"))
    );

    ArrayList<String> breadcrumb = pageBreadcrumb();
    for (int i = 0; i < breadcrumb.size(); i++) {
      String crumb = breadcrumb.get(i);
      innerContent += elementWithClass("li",
        "breadcrumb-item" + (i == (breadcrumb.size() - 1) ? " active" : ""), crumb);
    }

    return elementWithClass("ol", "breadcrumb float-sm-right", innerContent);
  }

  protected ArrayList<String> pageRightNavigation() {
    ArrayList<String> nav = new ArrayList<String>();
    nav.add(Html.spanWithId("ruby-running-status", Lte.formSubmitButton("danger",Html.fasIcon("spinner", "fa-pulse") + "Script Running"),
      Html.javascript("var rubyRunningStatus=function(status){if(status){document.getElementById('ruby-running-status').style.display='inline';}else{document.getElementById('ruby-running-status').style.display='none';}};rubyRunningStatus(" + (RubyScript.hasRunning()?"true":"false") + ");")));
    return nav;
  }

  protected boolean enableParentLink() {
    return true;
  }

  protected abstract ArrayList<Map.Entry<String, String>> pageNavigation();

  protected abstract String pageTitle();

  protected String pageSubTitle() {
    return "";
  }

  protected String pageTool() { return ""; };

  protected abstract ArrayList<String> pageBreadcrumb();

  protected abstract String pageContent();
}
