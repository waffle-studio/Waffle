package jp.tkms.waffle.component.template;

import jp.tkms.waffle.Environment;
import jp.tkms.waffle.component.*;

import java.util.ArrayList;

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
            (pageTitle() == "" ? Environment.APP_NAME : pageTitle() + " | " + Environment.APP_NAME)),
          link("stylesheet", "/css/adminlte.min.css"),
          link("stylesheet", "/css/fontawesome-free.min.css"),
          link("stylesheet", "/css/ionicons.min.css"),
          link("stylesheet", "/css/gfonts.css"),
          link("stylesheet", "/css/select2.min.css"),
          link("stylesheet", "/css/icheck-bootstrap.min.css"),
          link("stylesheet", "/css/toastr.min.css"),
          element("script", new Attributes(value("src", "/js/jquery.min.js")))
        ),
        body("hold-transition sidebar-mini",
          div("wrapper",
            elementWithClass("nav", "main-header navbar navbar-expand navbar-light",
              elementWithClass("ul", "navbar-nav",
                elementWithClass("li", "nav-item",
                  a("#", "nav-link", new Attributes(value("data-widget", "pushmenu")),
                    faIcon("bars")
                  )
                )/*,
                                elementWithClass("li", "nav-item d-none d-sm-inline-block",
                                    a("#", "nav-link", null, "T0")
                                ),
                                elementWithClass("li", "nav-item d-none d-sm-inline-block",
                                    a("#", "nav-link", null, "T1")
                                )*/
              )
            ),
            elementWithClass("aside", "main-sidebar sidebar-light-primary elevation-4",
              a(Environment.ROOT_PAGE, "brand-link navbar-light",
                new Attributes(value("title", "Workflow Administration Framework to Facilitate Looped Experiments")),
                attribute("img",
                  value("src", "/img/logo.png"),
                  value("class", "brand-image"),
                  value("style", "opacity:1.0;")
                ),
                span("brand-text text-warning font-weight-bold", null, Environment.APP_NAME)
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
                      a(JobsComponent.getUrl(), "nav-link", null,
                        faIcon("running", "nav-icon"),
                        p("Jobs")
                      )
                    ),
                    elementWithClass("li", "nav-item",
                      a(HostsComponent.getUrl(), "nav-link", null,
                        faIcon("server", "nav-icon"),
                        p("Hosts")
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
              element("strong", null, "Copyright &copy; 2019 S.T.")
            )
          ),
          element("script", new Attributes(value("src", "/js/bootstrap.bundle.min.js"))),
          element("script", new Attributes(value("src", "/js/adminlte.min.js"))),
          element("script", new Attributes(value("src", "/js/select2.min.js"))),
          element("script", new Attributes(value("src", "/js/toastr.min.js"))),
          element("script", new Attributes(value("src", "/js/simpleimport.js"))),
          element("script", new Attributes(value("type", "text/javascript")),
            "var bid=sessionStorage.getItem('bid');" +
              "if (bid == null) {bid = btoa(String.fromCharCode(...crypto.getRandomValues(new Uint8Array(32)))).replace(/[^a-zA-Z0-9]/g,'').substring(0,32);" +
              "sessionStorage.setItem('bid',bid);}"+
              "var loadBrowserMessage = function() {" +
              "simpleget('" + BrowserMessageComponent.getUrl("") + "' + bid, function(res) {try{eval(res)}catch(e){console.log(e)}setTimeout(loadBrowserMessage, 2000);})" +
              "}; " +
              "setTimeout(loadBrowserMessage, 2000);"
          ),
          element("script", new Html.Attributes( Html.value("src", "/ace/ace.js"), Html.value("type", "text/javascript")), ""),
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
              "    var editor = ace.edit(editDiv[0]);\n" +
              "    editor.renderer.setShowGutter(textarea.data('gutter'));\n" +
              "    editor.getSession().setValue(textarea.val());\n" +
              "    editor.getSession().setMode(\"ace/mode/\" + mode);\n" +
              "    editor.setTheme(\"ace/theme/textmate\");\n" +
              "    editor.resize();\n" +
              "    // copy back to textarea on form submit...\n" +
              "    textarea.closest('form').submit(function() {\n" +
              "      textarea.val(editor.getSession().getValue());\n" +
              "    })\n" +
              "  });\n" +
              "});")
        )
      )
    );
  }

  String randerPageBreadcrumb() {
    String innerContent = elementWithClass("li", "breadcrumb-item",
      a(Environment.ROOT_PAGE, null, null, faIcon("home"))
    );

    int size = pageBreadcrumb().size();
    for (int i = 0; i < size; i++) {
      String crumb = pageBreadcrumb().get(i);
      innerContent += elementWithClass("li",
        "breadcrumb-item" + (i == (size - 1) ? " active" : ""), crumb);
    }

    return elementWithClass("ol", "breadcrumb float-sm-right", innerContent);
  }

  protected abstract String pageTitle();

  protected String pageSubTitle() {
    return "";
  }

  protected abstract ArrayList<String> pageBreadcrumb();

  protected abstract String pageContent();
}