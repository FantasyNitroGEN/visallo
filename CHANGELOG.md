
#v2.2.0
==================

  * Add methods which take timestamps to SingleValueVisalloProperty and VisalloProperty

#v2.1
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
