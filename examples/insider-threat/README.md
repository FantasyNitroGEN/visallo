
1. Add the following to your `/opt/visallo/config/visallo.properties` file

        repository.ontology.owl.insiderThreat.iri=http://visallo.org/insider-threat
        repository.ontology.owl.insiderThreat.file=/home/jferner/dev/v5/project-c1/visallo-private/examples/insider-threat/ontology/insider-threat.owl

1. Start your Jetty web server
1. Run `org.visallo.tools.RdfImport -i examples/insider-threat/data/insider-threat-demo.nt`
