package org.visallo.core.http;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;

public class DefaultHttpRepository extends HttpRepository {
    @Inject
    public DefaultHttpRepository(Configuration configuration) {
        super(configuration);
    }
}
