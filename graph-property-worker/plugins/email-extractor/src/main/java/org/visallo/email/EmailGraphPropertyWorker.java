package org.visallo.email;

import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.ingest.graphProperty.RegexGraphPropertyWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.ontology.Concept;

@Name("E-Mail extractor")
@Description("Extracts E-Mail addresses from text")
public class EmailGraphPropertyWorker extends RegexGraphPropertyWorker {
    private static final String EMAIL_REG_EX = "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b";
    private Concept concept;

    public EmailGraphPropertyWorker() {
        super(EMAIL_REG_EX);
    }

    @Override
    protected Concept getConcept() {
        return concept;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        this.concept = getOntologyRepository().getConceptByIntent("email");
        if (this.concept == null) {
            throw new VisalloException("Could not find intent: email");
        }
        super.prepare(workerPrepareData);
    }
}
