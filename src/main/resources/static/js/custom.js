var gotoParent = function() {
    window.location.href = window.location.href.replace(/^(.*)\/.*$/, '$1');
};

if (sessionStorage.getItem('latest-project-id') != null) {
    document.getElementById('recently-accessed-project').innerHTML
        = "<a class='nav-link' title='recently accessed' href='/PROJECT/"
        + sessionStorage.getItem('latest-project-id')
        + "'><i class='nav-icon fas fa-angle-right' style='margin-lefti:1rem;'></i><p>"
        + sessionStorage.getItem('latest-project-name')
        + "</p></a>";
} else {
    document.getElementById('recently-accessed-project').style.display='none';
}

//KEEP STATUS OF A SIDEBAR
$('body').on('collapsed.lte.pushmenu', function(){document.cookie='sidebar=0;Path=/;SameSite=Strict;';});
$('body').on('shown.lte.pushmenu', function(){document.cookie='sidebar=1;Path=/;SameSite=Strict;';});

$(document).ready(function(){
    Array.prototype.forEach.call(document.getElementsByClassName("collapsed_state_key"), elem => {
        let key = "collapsed_state_key__" + elem.className.split("collapsed_state_key__")[1].split(" ")[0];
        $(elem).on("collapsed.lte.cardwidget", function () {
          document.cookie=key+'=1;Path=/;SameSite=Strict;';
        });
        $(elem).on("expanded.lte.cardwidget", function () {
          document.cookie=key+'=0;Path=/;SameSite=Strict;';
        });
    });
});
