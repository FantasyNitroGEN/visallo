$(document).off('.requeue').on('keyup.requeue', function (event) {
  var r = 'R'.charCodeAt(0);
  if (event.which === r && event.altKey && event.ctrlKey) {
    if (visalloData.selectedObjects.vertices.length) {
      console.log("requeuing " + visalloData.selectedObjects.vertices.length + " vertices");
      $.post('/requeue/vertex', {
        csrfToken: visalloData.currentUser.csrfToken,
        workspaceId: visalloData.currentWorkspaceId,
        vertexIds: _.pluck(visalloData.selectedObjects.vertices, 'id')
      })
    }

    event.preventDefault();
  }
});
