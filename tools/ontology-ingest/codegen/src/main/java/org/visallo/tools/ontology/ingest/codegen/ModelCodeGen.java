package org.visallo.tools.ontology.ingest.codegen;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.google.common.base.Strings;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.clientapi.JsonUtil;
import org.visallo.web.clientapi.UserNameAndPasswordVisalloApi;
import org.visallo.web.clientapi.UserNameOnlyVisalloApi;
import org.visallo.web.clientapi.VisalloApi;
import org.visallo.web.clientapi.model.ClientApiOntology;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

@Parameters(commandDescription = "Generate model classes based on a Visallo ontology.")
public class ModelCodeGen extends CommandLineTool {

    @Parameter(names = {"--inputJsonFile", "-f"}, arity = 1, converter = FileConverter.class, description = "The path to a local json file containing the Visallo ontology.")
    private File inputJsonFile;

    @Parameter(names = {"--visalloUrl", "-url"}, arity = 1, description = "The root URL of the Visallo instance from which to download the ontology.")
    private String visalloUrl;

    @Parameter(names = {"--visalloUsername", "-u"}, arity = 1, description = "The username to authenticate as when downloading the ontology from the Visallo instance.")
    private String visalloUsername;

    @Parameter(names = {"--visalloPassword", "-p"}, arity = 1, description = "The password to authenticate with when downloading the ontology from the Visallo instance.")
    private String visalloPassword;

    @Parameter(names = {"--outputDirectory", "-o"}, arity = 1, required = true, description = "The path to the output directory for the class files. If it does not exist, it will be created.")
    private String outputDirectory;

    @Parameter(names = {"--includeVisalloClasses"}, description = "By default, the core Visallo concepts and relationships are skipped during code generation. Include this flag to include them.")
    private boolean includeVisalloClasses;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new ModelCodeGen(), args, false);
    }

    @Override
    protected int run() throws Exception {
        String ontologyJsonString;
        if (inputJsonFile != null) {
            ontologyJsonString = new String(Files.readAllBytes(inputJsonFile.toPath()), Charset.forName("UTF-8"));
        } else if (!Strings.isNullOrEmpty(visalloUrl) && !Strings.isNullOrEmpty(visalloUsername) && !Strings.isNullOrEmpty(visalloPassword)) {
            VisalloApi visalloApi = new UserNameAndPasswordVisalloApi(visalloUrl, visalloUsername, visalloPassword, true);
            ontologyJsonString = visalloApi.invokeAPI("/ontology", "GET", null, null, null, null, "application/json");
            visalloApi.logout();
        } else if (!Strings.isNullOrEmpty(visalloUrl) && !Strings.isNullOrEmpty(visalloUsername)) {
            VisalloApi visalloApi = new UserNameOnlyVisalloApi(visalloUrl, visalloUsername, true);
            ontologyJsonString = visalloApi.invokeAPI("/ontology", "GET", null, null, null, null, "application/json");
            visalloApi.logout();
        } else {
            throw new VisalloException("inputJsonFile or visalloUrl, visalloUsername, and visalloPassword parameters are required");
        }

        ClientApiOntology ontology = JsonUtil.getJsonMapper().readValue(ontologyJsonString, ClientApiOntology.class);
        ConceptWriter conceptWriter = new ConceptWriter(outputDirectory, ontology, includeVisalloClasses);
        RelationshipWriter relationshipWriter = new RelationshipWriter(outputDirectory, ontology, includeVisalloClasses);

        ontology.getConcepts().forEach(conceptWriter::writeClass);
        ontology.getRelationships().forEach(relationshipWriter::writeClass);

        return 0;
    }
}
