require([
  'util/messages',
  'util/vertex/formatters',
  'util/requirejs/promise!util/service/loginPromise'
], function(i18n, F) {
  'use strict';

  if (visalloData.currentUser.privilegesHelper.ADMIN) {
    $(document)
      .trigger('registerKeyboardShortcuts', {
        scope: ['graph.help.scope', 'map.help.scope', 'search.help.scope'].map(i18n),
        shortcuts: {
          'ctrl-alt-R': {fire: 'requeueVertex', desc: 'Requeue a vertex'}
        }
      })
      .on('requeueVertex', function() {
        var count = visalloData.selectedObjects.vertices.length;
        if (count) {
          var countMessage = count + ' ' + (count === 1 ? 'Vertex' : 'Vertices');
          console.log('Requeuing ' + countMessage);
          var message = visalloData.selectedObjects.vertices.map(function(v) {
            return F.vertex.title(v);
          }).join(', ');
          var data = {
            csrfToken: visalloData.currentUser.csrfToken,
            workspaceId: visalloData.currentWorkspaceId,
            vertexIds: _.pluck(visalloData.selectedObjects.vertices, 'id')
          };
          $.post('/requeue/vertex', data, function() {
            $(document).trigger('postLocalNotification', {
              notification: {
                title: countMessage + ' Requeued',
                message: message
              }
            });
          });
        }
      });
  }
});
