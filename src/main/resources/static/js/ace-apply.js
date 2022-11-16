$(function() {
  $('textarea[data-editor]').each(function() {
    var textarea = $(this);
    var mode = textarea.data('editor');
    var snippetScript = textarea.data('snippet');
    var editDiv = $('<div>', {
      position: 'absolute',
      width: '100%',
      'class': textarea.attr('class')
    }).insertBefore(textarea);
    textarea.css('display', 'none');
    ace.require("ace/ext/language_tools");
    var editor = ace.edit(editDiv[0]);
    editor.renderer.setShowGutter(true);
    editor.getSession().setValue(textarea.val());
    editor.getSession().setMode("ace/mode/" + mode);
    editor.setTheme("ace/theme/textmate");
    editor.setOptions({enableBasicAutocompletion: true, enableSnippets: true, enableLiveAutocompletion: true, maxLines: Infinity});
    ace.config.loadModule('ace/snippets/snippets', function () {
        var snippetManager = ace.require('ace/snippets').snippetManager;
        ace.config.loadModule('ace/snippets/' + mode, function(m) {
            if (m) {
                snippetManager.files.ruby = m;
                m.snippets = snippetManager.parseSnippetFile(m.snippetText);
                m.snippets.push({
                    content: 'me.invokeListener("${1:listener name}")',
                    tabTrigger: 'me.invokeListener'
                });
                m.snippets.push({
                    content: 'me.loadConductorTemplate("${1:conductor template name}")',
                    tabTrigger: 'me.loadConductorTemplate'
                });
                m.snippets.push({
                    content: 'me.loadListenerTemplate("${1:listener template name}")',
                    tabTrigger: 'me.loadListenerTemplate'
                });
                m.snippets.push({
                    content: 'me.createExecutableRun("${1:executable name}", "${2:computer name}")',
                    tabTrigger: 'me.createExecutableRun'
                });
                m.snippets.push({
                    content: 'me.createConductorRun("${1:conductor name}")',
                    tabTrigger: 'me.createConductorRun'
                });
                m.snippets.push({
                    content: 'me.createProcedureRun("${1:procedure name}")',
                    tabTrigger: 'me.createProcedureRun'
                });
                m.snippets.push({
                    content: 'addFinalizer("${1:procedure name}")',
                    tabTrigger: 'addFinalizer'
                });
                m.snippets.push({
                    content: 'getResult("${1:key}")',
                    tabTrigger: 'getResult'
                });
                m.snippets.push({
                    content: 'makeLocalShared("${1:key}", "${2:file}")',
                    tabTrigger: 'makeLocalShared'
                });
                var s = function(trigger, content) {
                    m.snippets.push({
                        content: content,
                        tabTrigger: trigger
                    });
                };
                if (snippetScript != "") { eval(snippetScript); }                snippetManager.register(m.snippets, m.scope);
            }
        });
    });
    editor.resize();
    // copy back to textarea on form submit...
    textarea.closest('form').submit(function() {
      textarea.val(editor.getSession().getValue());
    })
  });
});