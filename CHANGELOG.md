v3.1.3
==================

## Added

* Retry-After header support to HttpRepository

## Changed

* Upgrade to Vertexium 2.5.7

## Fixed

* Image object resolution form no longer reopens every time you resize/move the selection box.
* Image selection box could not be moved if the Work Products List was closed.
* Issue where side panels would sometimes become invisible when trying to resize them.
* Property groups remain together in the property select
* Resolving object would sometimes fail.
* Video not able to be previewed in search results until something was selected

v3.1.2
==================

## Changed

* Stop workspace visibilities from being reported in history changes

## Fixed

* When editing a compound property different field values would sometimes switch if you only modified the visibility of the property.
* Importing files and viewing their history would show &#34;Created Thing&#34; instead of &#34;Created Image/Document/etc.&#34;

v3.1.1
==================

## Changed

* Performance improvements to users visiting with empty cache and connections with noticeable latency

## Fixed

* Always retrieve latest case diffs from the graph
* Brief display of some edges breaking the case sandbox when switching graphs
* Count aggregation on multiple properties. Unable to configure an aggregation if the previous aggregation used an incompatible visualization.
* Creates workspace edge when adding vertex
* Fixes issue with dashboard card data not rendering if the dashboard is
* Focus in dashboard card title field doesn&#39;t leave after typing pause
* Right clicking an item from a list in Firefox would not bring up the Context Menu.
* Subtitle/Time formula more visible when selected in element list

v3.1.0
==================

## Added

* Map configuration UI with [`org.visallo.map.options`](http://docs.visallo.org/extension-points/front-end/mapOptions/) extension point
* Cli tool to update the graph metadata version
* Configuration parameters to adjust the web server file upload limits. Set to 10MB / file, and 50MB / upload by default. `multipart.maxFileSize` and `multipart.maxRequestSize`
* JSONObject#null values to be serialized by objectmapper
* Multiple concepts and edge labels can be added to entity/relation search for an `IN` search
* Optional search filter to include child concepts in search results if filtering by a specific concept (defaults to true)
* Pushing onto the GraphPropertyWorker queue when a published property has been changed
* Scroll arrows to page entity and relationship tabs in the saved search table
* Search support for using `?` for single character wildcard queries
* Structured file ingest mapping interface. Ingest spreadsheets (csv, excel)
* Tomcat dependencies are inside of war when running with desktop profile
* Tool to run GPW from command line in isolation
* VisalloInMemoryTestBase to aid in creating tests
* WorkspaceListener class to listen to workspace repository events
* Work product relationships and properties to ontology
* `logQuiet` System property will use `log4j-quiet.xml` in configuration path
* Message when pasting elements
* Methods to update work product data and extended data
* `org.visallo.search.advanced` extension point with tutorial and documentation

## Changed

* `/resource` requests will inherit images from parent concepts
* Added empty relativePath to Visallo pom files whose parent pom is not located at `../pom.xml`
* Better title validation in work products
* Case titles are now checked to be unique only among your own cases, not those shared to you
* Collapse equal histogram detail values instead of range
* Enabled find related for multiple selected entities
* Enqueue a single item on to the queue when setting multiple properties at once
* Find path UI to allow easier selection of edge types
* GraphPropertyMessage to use Jackson for serialization
* GraphUpdateContext to use Vertexium&#39;s bulk save
* Handlebars templates are precompiled at build time
* Interfaces relating the Worker Queues
* Map and dashboard can accept file uploads
* Multi-select detail histogram bottom margin
* Prefetch messages WorkerBase and queue spouts to return byte[] instead of JSONObject
* Re-run the search if match-type (entity/relation) changes when there are no filters, but there is a search query
* Upgrade Vertexium to v2.5.3
* Webapp and plugins install dependencies with [yarn](https://yarnpkg.com) instead of npm
* When the other end of a relationship is not visible to the user, inspecting the relationships of a vertex will not include that vertex in the list
* WorkerBase no longer sleeps in between spout polling
* Changed worker status to be extensible

## Fixed

* Added an additional validation of the status when the graph property runner checks for whether or not any GPWs are interested.
* Case sharing username and permission dropdown overlap issues
* Changes to type filter when searching related will re-run the search
* Ctrl+click selection in Windows
* Dashboard card reports, such as the charts for entity counts and saved search aggregations, now return only the concept of the slice that is clicked in a popup search and not their child concepts
* Dashboard horizontal bar text clip colors in Firefox
* Date input fields would autocomplete user typing too early, making it difficult to enter a date without choosing from the calendar popup
* Don&#39;t show placeholder nodes loading for unauthorized or deleted entities
* Dropping elements in map products in the product list will open the map
* Empty timeline doesn&#39;t throw JavaScript exceptions
* Entities can be added and moved when snap to grid is enabled
* Entity context menu options still appeared when the number of selected entities did not match their configuration for the acceptable amount
* Entity selection in dashboard saved-search card
* Error message correctly displays for uploads exceeding size limits
* Find related properties missing
* Fix issue where loading the same relation saved search with a property filter would show duplicate filters
* Fixed Element Inspector add relationship comment menu item copy
* GPW streaming value temp file creation with invalid path char
* Graph and map product web app plugins were blank in active plugins list
* Graph fit will show all nodes even if the zoom is past preset minZoom
* Graph selection state could be unstable after changing the selection in the graph, then in external component, then in graph again
* Hide graph layout options when you don&#39;t have edit access to the case
* History was not available for relationships imported from RDF files
* In Firefox, when shift selecting rows in the saved search table, other text on the screen would be highlighted
* In IE, clicking the display all columns checkbox in the table configuration would not update on the first click if some columns were displayed and some were not(checkbox was indeterminate)
* In Internet Explorer, the find path configuration dialog was overflowing and cut off
* Issue where multiple selection element list could disappear
* JavaScript unit tests now run headless again on Mac OS Sierra (and possibly other env)
* Multiple selection undo last selection wouldn&#39;t close element inspector
* Ontology bug where dependent ontology files will modify parent relationship properties
* Priority not being propagated correctly
* Relationship lookup in ontology gets related properties
* Resolving detected objects from an image would require you to refresh to see the updates in the element inspector.
* Restore graph undo/redo using keyboard shortcut
* Saved search cards will now show configuration message instead of an error if the search they were configured to run has been deleted.
* Saving properties containing unicode characters on tomcat
* Saving snap to grid and find path settings
* Search by property value works correctly for edge properties
* Search filters no longer initially show both concept and edge type filters
* Search panel resubmits search queries when an extension filter is cleared without pressing enter in the search field
* Search panel resubmits search queries when the concept or relationship filter is cleared without pressing enter in the search field
* Search related icon on relationship sections in vertex detail panes no longer shows in fullscreen details where search is not available
* Select All keyboard shortcut was not working when trying to select lists
* Shift selecting multiple rows in the saved search table wasn&#39;t quite correct
* Structured ingest tool shows error when trying to map a property to more than one column
* Submit all types of saved search queries on selection
* TermMentionFilters work using SQL graph store
* The dashboard card configuration popover wasn&#39;t updating correctly, which sometimes made the card reset or lose tool options after configuring
* The properties automatically added to an image when you upload the image to another concept weren&#39;t showing the correct author and timestamp
* Timeline not accessible after activity pane is toggled
* Trying to ctrl+drag a connection between concepts on the graph and letting go while over a relationship would prevent further interaction
* Unresolve option was being displayed for term mentions/detected objects that were public entities which can&#39;t be unresolved
* Users without edit privilege won&#39;t see work product create /  delete / edit buttons
* VertexiumOntologyRepository returns properties on the parent concept when returning the parent concept
* When a vertex on a map product was changed to have a visibility you are not authorized to see, the map would not open
* When one or more of a relationship&#39;s vertices were not visible to the user, the element inspector would appear blank when trying to inspect that edge
* When the visibility of a published element changed to something you were not authorized to see and on a case you weren’t actively in, it would not disappear/reappear until the browser was refreshed
* ctrl/cmd+click was not deselecting already selected elements in a list
* Delete the product vertices when deleting the workspace vertex
* Race condition while adding work product ontology items
* Race condition while adding work products

## Documentation

* Added [JavaScript Public API](http://docs.visallo.org/javascript)
* Added documentation for running a cloud-based development environment
* Added tutorial and documented [`org.visallo.entity.listItemRenderer`](http://docs.visallo.org/extension-points/front-end/entityListItemRenderer/)
* Renamed Details Panel to Element Inspector
* How to ingest through REST, code generation, RDF NT + XML, and Vertexium with functional code examples
* New JSDoc documentation of front-end API, services, extension points
* New tutorials in documentation with references to actual plugin projects
* Updated ontology documentation with examples

## Deprecated

* `hbs!` requirejs plugin when requiring built-in (i.e. non-plugin) templates. Will show console message on how to fix.

## Removed

* Admin `dictionary`, `dictionaryDelete`, `dictionaryAdd` front-end services
* Conflicting inner dependencies of tika-parser
* Hierarchical graph layout for selections
* `org.visallo.detail.extensions` extension point, instead use the `org.visallo.layout.component` extension to adjust element inspector display

v3.0.0
==================

## Added

* Added route to get saved search on workspace
* Add map configuration UI with [`org.visallo.map.options`](http://docs.visallo.org/extension-points/front-end/mapOptions/) extension point
* Graph and Map are now &#34;Case Work Products&#34; within a workspace. Multiple of each type can be created within one workspace.


## Changed

* Collapse equal histogram detail values instead of range
* Geolocation properties are not sortable by default.
* Give default authorizations to all users, not just users without auths
* GraphUpdateContext to abstract to make it clear that you should get the instance from GraphRepository
* Multi-select detail histogram bottom margin
* Remove Exception from GraphUpdateContext#close to avoid needing to catch
* Speed up ACLProvider by caching the ontology property IRI
* Speed up ACLProvider by not repeatly calling hasPrivilege
* Upgrade Accumulo from 1.6.1 to 1.6.6
* Upgrade Accumulo from 1.6.6 to 1.7.2
* Upgrade Elasticsearch from 1.7.2 to 1.7.5
* Upgrade Hadoop from CDH5.4.2 to 2.7.3
* Upgrade V5 dependencies to support new infrastructure versions
* Upgrade ZooKeeper from 3.4.7 to 3.4.9
* Webapp and plugins install dependencies with [yarn](https://yarnpkg.com) instead of npm
* Webstart version from v2.2.0 to v2.2.1

## Fixed

* Delete the product vertices when deleting the workspace vertex
* Fix issue where decorations would not position correctly after vertex update
* Fix SearchHelper switching after workspace search removed
* GPW streaming value temp file creation with invalid path char
* Added listeners to update the card locations on the server since there were certain instances in which the card locations were not updated and saved to the server.
* After a successful logout, a &#39;Server is not available&#39; message would appear.
* Find related properties missing
* Multiple selection histogram would sometimes have overlapping number/text
* Offline overlay not appearing when the browser loses network connection.
* Relationship lookup in ontology gets related properties
* Relationship lookup in ontology gets related properties
* Search related icon on relationship sections in vertex detail panes no longer shows in fullscreen details where search is not available.
* Users without edit privilege won&#39;t see work product create /  delete / edit buttons
* detail pane showing comment reply buttons and add property/comment toolbar items
* graph images in IE
* graph.flush calls on ontology update methods to prevent race conditions

## Removed

* `active` property is no longer included in `workspace/all` response for easy change tracking
* `pluginDevMode` is no longer available. Turn off compilation on a per-file basis with `registerCompiledJavaScript` or `skipCompile`
* Add to Workspace URL scheme has been removed as entities can&#39;t be added to a workspace.
* Statistics aggregation option in saved search dashboard cards. This aggregation type will be reworked in a future release.
* Workspace search is removed since workspaces don&#39;t have entities

v2.2.2
==================

## Changed

* User account management modal to use an user’s display name rather
  than username

## Fixed

* Prevent notification groups to get dismissed inadvertently
* Property info popover not opening when clicking on another user’s
  comment.

v2.2.1
==================

## Added

* Add the ability to configure development web servers to ask for a
  client certificate (i.e. want), without requiring one.
* Data/display type formatters now return the element.

## Changed

* Added method to allow directory searches for people to return an email
  attribute
* Default authorizations will be given to all users, not just users
  without any existing authorizations.
* Provide options for restricting directory search to people and/or
  groups

## Fixed

* Dashboard card components no longer stick around when they’re not
  supposed to.
* Dashboard does not freeze anymore when cards are moved vertically

v2.2.0
==================

## Added

* HISTORY_READ privilege required for viewing vertex/edge/property history
* An archetype jar to help developers generate a maven project to start plugin development.
* OwlToJava: Support StreamingPropertyValue and DirectoryEntity types
* Allow admin extensions to request a sort within a section
* Provide a way to redirect the user after authenticating
* Add methods which take timestamps to SingleValueVisalloProperty and VisalloProperty
* Include babel polyfill https://babeljs.io/docs/usage/polyfill/

## Changed

* Upgraded React from 0.14.8 to 15.3.0
* version branch 2.2 depends on the vertexium 2.4.5
* Refactor web visibility validation to VisibilityValidator class
* Web: Merge logic to get client IP address from CurrentUser and AuthenticationHander
* Allow ACLProvider to continue even if a concept or relationship cannot be found

## Fixed

* Add user admin privilges plugin to visallo-dev-jetty-server module
* Dashboard pie chart infinite loop

## Documentation

* Steps to generate the archetype jar

## Deprecated

* getRemoteAddr to provide a more consistent way of retrieving the client IP address. Use RemoteAddressUtil.getClientIpAddr to get the client IP address in the future.

v2.1.2
==================

## Fixed

* Issue where search property filters did not show up in IE.
* Search filters had a bug due to using a single equals rather than
  comparing with triple equals.

v2.1.1
==================

## Changed

* Only show available properties that are sortable (specified in
  ontology) in search pane sort inv.
* Pass vertex to `shouldDisable` handler for `org.visallo.vertex.menu`
  extensions
* The `org.visallo.detail.text` front-end extension function
  `shouldReplaceTextSectionForVertex` is now passed property name and
key

## Fixed

* Activity pane now shows correctly when multiple activity extensions
  are registered.
* Admin user editor now correctly resets authorization list when
  switching between users
* _Find Path_ actions in activity pane update correctly when multiple
  rows have the same source and destination vertices.
* No longer offer string properties as available properties to aggregate
  on in histograms (only dates and numbers are supported)
* Property select field not disabling properly when there are no
  available properties.
* Vertexium user property map not being updated through Proxy User
* When creating a connection in the graph, the preview edge arrow
  displays correctly after inverting direction
* Workspace sharing between users with published entities might
  disappear for some users on update.

## Documentation

* Add `shouldDisable` example to
  [`org.visallo.vertex.menu`](http://docs.visallo.org/extension-points/front-end/vertexMenu/)
* Extension documentation for
  [`org.visallo.detail.text`](http://docs.visallo.org/extension-points/front-end/detailText/)
* Update web app plugin tutorial to use public API and fix errors

## Removed

* FormatVisallo CLI tool

v2.1.0
==================

  * Always update ontology in evaluator context
  * Upgrade jetty to 9.2.9.v20150224
  * Fix double import when using enter to import file
  * Add filters to aggregation property selects and check for mapzen
  * Fix issues so saved searches can switch between global and private with the appropriate user privilege
  * Fix accumulo security exception when access is deleted from another user
  * Fix flashing of right aligned numbers in multiple selection histogram detail pane.
  * Truncate the histogram text so count number is visible
  * RDF Export: add rdf comment to exported file for unsupported data types
  * Fix acl check for compound properties
  * Escape quotes exported RDF literals
  * Fix bug with importing strings with quotes in them from rdf
  * RDF Import: Fix metadata import, mutation not being updated to a ExistingElementMutation correctly
  * Add visallo system metadata to elements and properties when importing using xml rdf (#541)
  * Remove includes as IE doesn't have it
  * UserAdminCLI: Support setting authorizations and privileges from the user admin CLI
  * Add popover positioning to map component
  * Protect against NPE's when checking workspace access. Happens when shared workspace deleted
  * Fix dashboard saved search still loading when limit is set
  * Match visallo dep to poi with tika's dep version. Fixes importing docx
  * Fix firefox text cutting off at end of dashboard bar charts
  * Use init parameters when creating configuration
  * Change exec-maven-plugin group id from org.apache.maven.plugins to org.codehaus.mojo.
  * Remove dialogs and popovers on session expiration
  * Remove text that says delete will remove relationship
  * Fix syntax error in style url background-image
  * Fix some race conditions with text property updating while in the process of opening another text
  * Fix uploaded image not being pushed onto queue
  * MetricsManager in RdfTripleImportHelper
  * Configuration: Support setting system properties via Visallo configuration properties
  * Fix exceptions in iterator finalize
  * Fix dashboard missing extension toolbar items when on shared workspace dashboard
  * Add option requiredPrivilege to registerExtension to suppress extension based on privileges
  * Add handlers to toggle registered keyboard shortcuts
  * Correctly add a vertex to shared workspace if visibility is change such that the shared workspace can now see it
  * User must have SEARCH_SAVE_GLOBAL privilege to save/delete
  * Add the server's host name to the ping vertex id
  * Upgrade Vertexium from 2.4.3 to 2.4.4
  * User admin styling post-refactor fixes
  * Use component attacher for menubar extensions
  * RDF: Increase RDF triple import performance by hanging onto the Vertexium mutations between lines
  * Remove unnecessary edge filtering which broke with published edges
  * Stickier user notifications
  * Hide search hits label after closing pane
  * Add new user auths and default auths to graph
  * Fix acl add/update/delete checks on comment properties
  * Adjust popover z-index to appear on top of closest modal
  * Uglify production builds while correctly concatenating sourcemap from babel
  * Use babel to transpile all js, not just jsx.
  * Fix UI issue if no creator of the workspace is found
  * Fix detectedObjects & properties with long text causing detail overflow
  * Refactor Authorizations and Privileges out of UserRepository into AuthorizationRepository and PrivilegeRepository
  * Visibility validation and highlighting on term and import forms
  * Highlighting for invalid comment visibility input
  * Add clientIpAddress to log4j MDC
  * Fix babel plugin compilation for jsx
  * Move Apache Poi version to root pom
  * Fix NPE when calling toString on ClientApiWorkspace when users or vertices are null
  * Add workspace and user title formulas. Add concept type properties to dashboard and dashboard item vertices
  * RDF: Support raw (not transformed by VisibilityTranslator) visibility.
  * Move eslint and set root flag
  * Add ConfigDirectoryStaticResourceHandler helper class to load files and serve them from the Visallo config directories
  * Fix z-index calculation for multiple stacked modals
  * Upgrade tika to 1.13 and commons compress to 1.11 to match tika dependency
  * Add additional checks to ACLProvider to help troubleshoot
  * Keep track of open vertex previews
  * Add hash visibility source to TM vertex id if it exists
  * Update grunt deps (grunt from ^0.4.5 to ^1.0.1 and grunt-exect from ^0.4.6 to ^1.0.0)
  * Adds documentation for web.cacheServletFilter.maxAge property
  * Document pluginDevMode in dev property file
  * Eslint upgrade (eslint-plugin-react to ^5.2.2 and grunt-eslint to ^19.0.0)
  * Add object rest/spread support plugin to babel
  * Fix edit visibility of text when collapsed
  * Ignore hidden legend labels when ellipsizing
  * Create more consistant metadata in GPWs, to include modified by and modified date
  * Validate visibility fields
  * Upgrade to gitbook 3
