package pt.ua.imodec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.JettyPluginInterface;
import pt.ua.dicoogle.sdk.PluginSet;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.imodec.webservice.ImodecJettyPlugin;

import java.util.Arrays;
import java.util.Collection;

public class ImodecPluginSet implements PluginSet {

    private static final Logger logger = LoggerFactory.getLogger(ImodecPluginSet.class);

    // Plugins as attributes to the plugin set
    private final ImodecJettyPlugin jettyWeb;

    // Additional resources
    // ...
    private ConfigurationHolder settings;

    public ImodecPluginSet() {
        logger.info("Initializing Imodec Plugin Set");

        this.jettyWeb = new ImodecJettyPlugin();

        logger.info("Sample Plugin Set is ready");
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Collection<JettyPluginInterface> getJettyPlugins() {
        return Arrays.asList(this.jettyWeb);
    }

    @Override
    public void setSettings(ConfigurationHolder xmlSettings) {
        this.settings = xmlSettings;
    }

    @Override
    public ConfigurationHolder getSettings() {
        return settings;
    }

}
