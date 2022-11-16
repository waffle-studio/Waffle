$(function() {
  $('textarea[data-jsoneditor]').each(function() {
    var textarea = $(this);
    var mode = textarea.data('jsoneditor');
    var textmode = (mode=='view'?'preview':'code');
    var editDiv = $('<div>', {
      width: '100%',
    }).insertBefore(textarea);
    textarea.css('display', 'none');
    var editor = new JSONEditor(editDiv[0], {"onModeChange":function(mode){if(mode=='code'){editor.aceEditor.setOptions({maxLines: Infinity})}},"ace":ace,"language":"en","statusBar":false,"navigationBar":false,"enableTransform":false,"enableSort":false,"search": false,"autocomplete":{getOptions:function(){return [];}},"modes":[mode,textmode]}, JSON.parse(textarea.val()));
    //editor.editor.setOptions({maxLines: Infinity});
    // copy back to textarea on form submit...
    textarea.closest('form').submit(function() {
      textarea.val(editor.getText());
    })
  });
});
