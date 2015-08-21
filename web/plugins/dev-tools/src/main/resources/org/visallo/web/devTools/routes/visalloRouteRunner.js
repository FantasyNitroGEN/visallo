var userMe;

var oldLoadRoute = loadRoute;
loadRoute = function (method, path) {
  oldLoadRoute(method, path);
  addHeaderParameter('Visallo-Workspace-Id', userMe.currentWorkspaceId);
  if (method != 'GET') {
    addHeaderParameter('Visallo-CSRF-Token', userMe.csrfToken);
  }
};

var refreshUserMe = function () {
  var httpRequest;
  if (window.XMLHttpRequest) { // Mozilla, Safari, IE7+ ...
    httpRequest = new XMLHttpRequest();
  } else if (window.ActiveXObject) { // IE 6 and older
    httpRequest = new ActiveXObject("Microsoft.XMLHTTP");
  }
  httpRequest.onreadystatechange = function () {
    if (httpRequest.readyState == 4) {
      userMe = JSON.parse(httpRequest.responseText);
    }
  };
  httpRequest.open('GET', '/user/me', true);
  httpRequest.send(null);
};

refreshUserMe();
