# Maven Archetype for Visallo Plugin Development

This project is a [Maven Archetype](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html) designed to help outside developers get started with writing Visallo plugins. It provides a recommended Maven project structure and a working set of plugin modules (UI, Graph Property Worker, and Authentication). Developers can use these modules as starting points for creating their own plugins.

### Testing the Archetype

When `mvn install` is run, a sample project is automatically generated from the archetype as a convenient way to test it. Use the following commands to fully build the archetype and run Visallo using the generated sample project:
```
mvn install
cd target/test-classes/projects/basic/project/basic
./run.sh
```
