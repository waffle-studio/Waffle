function s_id(id) {
    return document.getElementById(id);
}

function s_hide(el) {
    el.style.display = 'none';
}

function s_showib(el) {
    el.style.display = 'inline-block';
}

function s_put(el, val) {
    el.innerHTML = val;
}

var updateJobNum = function(n) {
  if (n > 0) {
      document.getElementById('jobnum').innerHTML = n;
  } else {
  }
}
