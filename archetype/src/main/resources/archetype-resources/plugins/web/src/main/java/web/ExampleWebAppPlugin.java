#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.web;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Example Visallo Web App Plugin")
@Description("Registers a detail toolbar plugin that launches a Google search for the displayed person name.")
public class ExampleWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerJavaScript("/${packageInPathFormat}/web/plugin.js", true);
        app.registerResourceBundle("/${packageInPathFormat}/web/messages.properties");
    }
}
