
1. Configure has media edge types in your `visallo.properties` file, these IRIs represent the edge labels from an entity to an images. e.g.:

        assignImageMR.hasImageLabel.0=http://visallo.org/palantir-import#hasMedia

1. Run:

        yarn jar visallo-assign-image-mr-*-SNAPSHOT-jar-with-dependencies.jar
