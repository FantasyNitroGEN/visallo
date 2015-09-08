define([
  'flight/lib/component'
], function (defineComponent) {
  'use strict';

  return defineComponent(RouteRunner);

  function RouteRunner() {
    this.after('initialize', function () {
      var win = window.open('admin/routeRunner', '_blank');
      win.focus();
    });
  }
});
