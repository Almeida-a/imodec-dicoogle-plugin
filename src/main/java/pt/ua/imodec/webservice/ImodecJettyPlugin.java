package pt.ua.imodec.webservice;

import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.JettyPluginInterface;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/**
 * Jetty Servlet plugin, based on bioinformatics-ua/dicoogle-plugin-sample
 *
 * @author André Almeida - <almeida.a@ua.pt>
 * */

public class ImodecJettyPlugin implements JettyPluginInterface, PlatformCommunicatorInterface {

    private static final Logger logger = LoggerFactory.getLogger(ImodecJettyPlugin.class);
    private final ImodecJettyWebService webService;
    private boolean enabled;
    private ConfigurationHolder settings;
    private DicooglePlatformInterface platform;

    public ImodecJettyPlugin() {
        this.webService = new ImodecJettyWebService();
        this.enabled = true;
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface pi) {
        this.platform = pi;

        this.webService.setPlatformProxy(pi);
    }

    @Override
    public String getName() {
        return "plugin-jetty";
    }

    @Override
    public boolean enable() {
        this.enabled = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.enabled = false;
        return false;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public ConfigurationHolder getSettings() {
        return settings;
    }

    @Override
    public void setSettings(ConfigurationHolder settings) {
        this.settings = settings;
    }

    /**
     * Sets the servlets and their paths through which they are accessed.
     *
     * Example below to remind how the path is formed.
     * */
    @Override
    public HandlerList getJettyHandlers() {

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/imodec");
        handler.addServlet(new ServletHolder(this.webService), "/hello");
        // Example: path to access this servlet? example below
        // GET http://localhost:8080/imodec/hello?param=value

        HandlerList l = new HandlerList();
        l.addHandler(handler);

        return l;
    }

}
