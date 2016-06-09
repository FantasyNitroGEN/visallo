package org.visallo.tools.format;

import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.core.util.ModelUtil;

@Parameters(commandDescription = "Deletes all data from Visallo")
public class FormatVisallo extends CommandLineTool {
    private GraphAuthorizationRepository graphAuthorizationRepository;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new FormatVisallo(), args);
    }

    @Override
    protected int run() throws Exception {
        ModelUtil.drop(
                getGraph(),
                getSimpleOrmSession(),
                getUserRepository(),
                getWorkQueueRepository(),
                graphAuthorizationRepository,
                getUser()
        );
        return 0;
    }

    @Inject
    public void setGraphAuthorizationRepository(GraphAuthorizationRepository graphAuthorizationRepository) {
        this.graphAuthorizationRepository = graphAuthorizationRepository;
    }
}
