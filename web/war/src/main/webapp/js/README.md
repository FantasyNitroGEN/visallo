
# JavaScript Source Tree

The `index.hbs` page loads [require.js](http://requirejs.org) with the configuration in `require.config.js` and loads subsequently loads `visallo.js`. This is the main entry point to Visallo.

Visallo then transitions into one of three possible states based on url fragment identifier and cookies:

* Login
* Case
* Fullscreen

## Login

File: `login.js`

The login page is shown when the users session is expired or missing. It renders the login page along with the authentication plugin to login the user, at which point it transitions to the Case or Fullscreen view.


## Case

File: `app.js`

The case view of Visallo is the default view. It contains the main application, with a graph, map, detail pane, and ancillary panes.

If the fragment identifier contains `#w=`, the user is prompted to add entities to their case after load.

        https://try.visallo.org#w=[ vertexId_1 [, vertexId_2 ] ]

## Fullscreen

File: `appFullscreenDetails.js`

The fullscreen view is a view of one or many entities in a grid. The entities are displayed using the same component as they are shown in the case when selecting them. The entities loaded are loaded using the fragment identifier.

        https://try.visallo.org#v=[ vertexId_1 [, vertexId_2 ] ]
