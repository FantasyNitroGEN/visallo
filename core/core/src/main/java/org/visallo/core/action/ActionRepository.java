package org.visallo.core.action;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class ActionRepository {
    private final Collection<Action> actions;

    @Inject
    public ActionRepository(
            Configuration configuration
    ) {
        actions = InjectHelper.getInjectedServices(Action.class, configuration);
    }

    public Action getActionFromActionData(JSONObject json) {
        String type = json.getString(Action.PROPERTY_TYPE);
        for (Action action : actions) {
            if (action.getClass().getName().equals(type)) {
                return action;
            }
        }
        return null;
    }

    public void checkActionData(JSONObject actionData) {
        checkNotNull(actionData, "actionData cannot by null");
        Action action = getActionFromActionData(actionData);
        checkNotNull(action, "Could not find action for data: " + actionData.toString());
        action.validateData(actionData);
    }
}
