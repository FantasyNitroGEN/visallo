
Use the following command to execute all integration tests, using a locally running development docker instance:

```
mvn -am -pl integration-test test -PITest \
    -Dtest=*IntegrationTest \
    -DfailIfNoTests=false \
    -DtestServer=visallo-dev \
    -Drepository.ontology=org.visallo.vertexium.model.ontology.InMemoryOntologyRepository
```
