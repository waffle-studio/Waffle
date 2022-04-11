var gotoParent = function() {
    window.location.href = window.location.href.replace('/^(.*)/.*$/','$1');
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
