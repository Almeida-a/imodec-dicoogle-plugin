package pt.ua.imodec;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.dcm4che2.data.TransferSyntax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.JettyPluginInterface;
import pt.ua.dicoogle.sdk.PluginSet;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.imodec.storage.ImodecStoragePlugin;
import pt.ua.imodec.datastructs.formats.Format;
import pt.ua.imodec.datastructs.formats.Native;
import pt.ua.imodec.datastructs.formats.NewFormat;
import pt.ua.imodec.webservice.ImodecJettyPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
    public static final Path TMP_DIR_PATH = Paths.get("/tmp/imodec");

    private ConfigurationHolder settings;

    public ImodecPluginSet() {
        this.jettyWeb = new ImodecJettyPlugin();
        this.storage = new ImodecStoragePlugin();

        tmpMkdirs();

        logger.info("Imodec Plugin Set is ready");
    }

    private static void tmpMkdirs() {
        if (!TMP_DIR_PATH.toFile().mkdirs())
            logger.info("Could not create main tmp directory. It already exists");
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

        if (chosenFormat == null) {
            setImageCompressionFormat(xmlSettings);
            setEncoderOptions(xmlSettings);
        }

        this.settings = xmlSettings;
    }

    private void setEncoderOptions(ConfigurationHolder xmlSettings) {

        List<ConfigurationNode> configurationNodeList = xmlSettings
                .getConfiguration()
                .getRootNode().getChildren();

        for (ConfigurationNode tag :
                configurationNodeList) {
            Optional<NewFormat> format = Arrays.stream(NewFormat.values())
                    .filter(format1 -> format1.getFileExtension().equals(tag.getName()))
                    .findFirst();

            if (!format.isPresent())
                continue;

            NewFormat format1 = format.get();
            Optional<ConfigurationNode> attributeQuality = tag.getAttributes()
                    .stream()
                    .filter(configurationNode -> configurationNode.getName().equals(format1.getQualityParamName()))
                    .findFirst();
            Optional<ConfigurationNode> attributeSpeed = tag.getAttributes()
                    .stream()
                    .filter(configurationNode -> configurationNode.getName().equals(format1.getSpeedParamName()))
                    .findFirst();

            if (attributeQuality.isPresent()) {
                try {
                    Float value = Float.valueOf((String) attributeQuality.get().getValue());
                    logger.debug("Format '{}' quality option '{}' was changed to '{}'",
                            format1.getId(), format1.getQualityParamValue(), value);
                    format1.setQualityParamValue(value);
                } catch (ClassCastException | NumberFormatException ignored) {
                    logger.warn("Invalid quality value for '{}' -> '{}'." +
                                    " Maintaining default options.",
                            format1.getFileExtension(), attributeQuality.get().getValue());
                }
            }

            if (attributeSpeed.isPresent()) {
                try {
                    Float value = Float.valueOf((String) attributeSpeed.get().getValue());
                    logger.debug("Format '{}' speed option '{}' was changed to '{}'",
                            format1.getId(), format1.getSpeedParamValue(), value);
                    format1.setSpeedParamValue(value);
                } catch (ClassCastException | NumberFormatException ignored) {
                    logger.warn("Invalid speed value for '{}' -> '{}'." +
                                    " Maintaining default options.",
                            format1.getFileExtension(), attributeSpeed.get().getValue());
                }
            }
        }
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
