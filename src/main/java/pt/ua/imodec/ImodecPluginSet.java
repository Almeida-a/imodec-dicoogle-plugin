package pt.ua.imodec;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.dcm4che2.data.TransferSyntax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.JettyPluginInterface;
import pt.ua.dicoogle.sdk.PluginSet;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.imodec.storage.ImodecStoragePlugin;
import pt.ua.imodec.util.formats.Format;
import pt.ua.imodec.util.formats.Native;
import pt.ua.imodec.util.formats.NewFormat;
import pt.ua.imodec.webservice.ImodecJettyPlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * Template based on <a href="https://github.com/Almeida-a/dicoogle-plugin-sample/blob/experimental/src/main/java/pt
 * /ieeta/dicoogle/plugin/sample/SamplePluginSet.java">this</a>.
 * <p>
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
    public static Format chosenFormat = null;
    private ConfigurationHolder settings;

    public ImodecPluginSet() {
        this.jettyWeb = new ImodecJettyPlugin();
        this.storage = new ImodecStoragePlugin();

        logger.info("Imodec Plugin Set is ready");
    }

    @Override
    public String getName() {
        return "imodec-plugin-set";
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

        if (chosenFormat == null)
            setImageCompressionFormat(xmlSettings);

        this.settings = xmlSettings;
    }

    private static void setImageCompressionFormat(ConfigurationHolder xmlSettings) {

        Format defaultFormat = Native.UNCHANGED;

        String chosenFormatId = xmlSettings.getConfiguration().getString("codec");

        if (chosenFormatId.equals("all")) {
            chosenFormat = new Format() {
                @Override
                public TransferSyntax getTransferSyntax() {
                    return null;
                }

                @Override
                public String getId() {
                    return "all";
                }
            };
        } else
            chosenFormat = Arrays.stream(((Format[]) NewFormat.values()))
                    .filter(newFormat -> newFormat.getId().equals(chosenFormatId))
                    .findFirst()
                    .orElse(defaultFormat);

        logger.info(
                String.format("Format requested: '%s', set -> '%s'",
                        chosenFormatId, chosenFormat.getId())
        );
    }

    @Override
    public ConfigurationHolder getSettings() {
        return settings;
    }

}
