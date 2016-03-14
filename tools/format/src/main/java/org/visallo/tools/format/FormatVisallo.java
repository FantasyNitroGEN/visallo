package org.visallo.tools.format;

import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.util.ModelUtil;

@Parameters(commandDescription = "Deletes all data from Visallo")
public class FormatVisallo extends CommandLineTool {
    private AuthorizationRepository authorizationRepository;

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
                authorizationRepository,
                getUser()
        );
        return 0;
    }

    @Inject
    public void setAuthorizationRepository(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }
}
