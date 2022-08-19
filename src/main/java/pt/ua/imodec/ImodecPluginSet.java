package pt.ua.imodec;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.JettyPluginInterface;
import pt.ua.dicoogle.sdk.PluginSet;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.imodec.storage.ImodecStoragePlugin;
import pt.ua.imodec.webservice.ImodecJettyPlugin;

import java.util.Collection;
import java.util.Collections;

/**
 *
 * Template based on <a href="https://github.com/Almeida-a/dicoogle-plugin-sample/blob/experimental/src/main/java/pt
 * /ieeta/dicoogle/plugin/sample/SamplePluginSet.java">this</a>.
 *
 * This is the entry point for all plugins
 *
 *  @author Luís A. Bastião Silva - <bastiao@ua.pt>
 *  @author Eduardo Pinho <eduardopinho@ua.pt>
 *  @author Rui Lebre - <ruilebre@ua.pt>
 *
 * */

@PluginImplementation
public class ImodecPluginSet implements PluginSet {

    private static final Logger logger = LoggerFactory.getLogger(ImodecPluginSet.class);

    // Plugins as attributes to the plugin set
    private final ImodecJettyPlugin jettyWeb;
    private final ImodecStoragePlugin storage;

    // Additional resources
    // ...
    private ConfigurationHolder settings;

    public ImodecPluginSet() {
        logger.info("Initializing Imodec Plugin Set");

        this.jettyWeb = new ImodecJettyPlugin();
        this.storage = new ImodecStoragePlugin();

        logger.info("Sample Plugin Set is ready");
    }

    @Override
    public String getName() {
        return "Imodec plugin set";
    }

    @Override
    public Collection<JettyPluginInterface> getJettyPlugins() {
        return Collections.singletonList(this.jettyWeb);
    }

    @Override
    public Collection<StorageInterface> getStoragePlugins() {
        return Collections.singletonList(this.storage);
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
