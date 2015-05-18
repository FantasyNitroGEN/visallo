package org.visallo.translate;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import org.visallo.core.bootstrap.BootstrapBindingProvider;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.ClassUtil;

public class TranslateBootstrapBindingProvider implements BootstrapBindingProvider {
    private static final String CONFIG_TRANSLATOR_CLASS_NAME = "translate.translator";

    @Override
    public void addBindings(Binder binder, Configuration configuration) {
        String translatorClassName = configuration.get(CONFIG_TRANSLATOR_CLASS_NAME, NopTranslator.class.getName());
        try {
            Class<? extends Translator> translatorClass = ClassUtil.forName(translatorClassName);

            binder.bind(Translator.class)
                    .to(translatorClass)
                    .in(Scopes.SINGLETON);
        } catch (Exception ex) {
            throw new VisalloException("Could not bind translator: " + translatorClassName, ex);
        }
    }
}
