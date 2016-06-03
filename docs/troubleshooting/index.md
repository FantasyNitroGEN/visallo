# Troubleshooting

## A Graph Property Worker does not seem to be working:

There could be many specific problems as to why your Graph Property Worker is not working.  Check the following:

* Ensure that the plugin shows up in the list of plugins in the Admin Panel.  If it is not there, it is not on the classpath so it must be placed on the classpath.
* Verify that there were no errors in the logs while the graph property workers were starting up.  If there are errors in one of the graph property workers, no data will get sent to any of them.

