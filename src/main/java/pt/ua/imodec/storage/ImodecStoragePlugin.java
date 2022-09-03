package pt.ua.imodec.storage;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.imodec.ImodecPluginSet;
import pt.ua.imodec.util.ImageUtils;
import pt.ua.imodec.util.MiscUtils;
import pt.ua.imodec.util.formats.Format;
import pt.ua.imodec.util.formats.NewFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 *
 * Storage Plugin
 *  - "Template" from rlebre/dicoogle-plugin-sample
 * <p>
 * */
public class ImodecStoragePlugin implements StorageInterface {

    private static final Logger logger = LoggerFactory.getLogger(ImodecStoragePlugin.class);

    private final HashMap<String, ByteArrayOutputStream> mem = new HashMap<>();
    private boolean enabled = true;
    private ConfigurationHolder settings;

    @Override
    public String getScheme() {
        return "imodec-mem";
    }

    @Override
    public Iterable<StorageInputStream> at(final URI location, Object... objects) {

        return () -> {
            Collection<StorageInputStream> c2 = new ArrayList<>();
            StorageInputStream s = new StorageInputStream() {
                @Override
                public URI getURI() {
                    return location;
                }

                @Override
                public InputStream getInputStream() {
                    ByteArrayOutputStream bos = mem.get(location.toString());

                    if (bos == null)
                        throw new NoSuchElementException(
                                String.format("File uri='%s' was not found at the storage!", location)
                        );

                    return new ByteArrayInputStream(bos.toByteArray());
                }

                @Override
                public long getSize() {
                    return mem.get(location.toString()).size();
                }
            };
            c2.add(s);
            return c2.iterator();
        };
    }

    @Override
    public URI store(DicomObject dicomObject, Object... objects) {

        logger.warn("Waiting while format is being set");

        Supplier<Boolean> choosingProcess = () -> ImodecPluginSet.chosenFormat == null;
        MiscUtils.sleepWhile(choosingProcess);
        Format chosenFormat = ImodecPluginSet.chosenFormat;

        URI uri = getUri(dicomObject);
        if (mem.containsKey(uri.toString())) {
            logger.warn("This object was already stored!");
            return uri;
        }

        try {
            if (chosenFormat instanceof NewFormat) {
                ImageUtils.encodeDicomObject(dicomObject, (NewFormat) chosenFormat);
            }
            else if (chosenFormat.getId().equals("all") && objects.length == 0) {
                // TODO: 03/09/22 Find a better way for stopping condition than by the number of argument objects
                DicomObject[] dicomObjects = ImageUtils.encodeDicomObjectWithAllTs(dicomObject);
                for (DicomObject dicomObject1 : dicomObjects) {
                    store(dicomObject1, new Object());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DicomOutputStream dos = new DicomOutputStream(bos);
        try {
            dos.writeDicomFile(dicomObject);
        } catch (IOException ex) {
            logger.warn("Failed to store object", ex);
        }
        bos.toByteArray();
        mem.put(uri.toString(), bos);

        return uri;
    }

    private URI getUri(DicomObject dicomObject) {
        return URI.create(getScheme() + "://"
                + dicomObject.getString(Tag.SOPInstanceUID) + "/"
                + dicomObject.getString(Tag.TransferSyntaxUID));
    }

    @Override
    public URI store(DicomInputStream dicomInputStream, Object... objects) throws IOException {
        return store(dicomInputStream.readDicomObject());
    }

    @Override
    public void remove(URI location) {
        this.mem.remove(location.toString());
    }

    @Override
    public String getName() {
        return "imodec-storage-plugin";
    }

    @Override
    public boolean enable() {
        this.enabled = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.enabled = false;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setSettings(ConfigurationHolder settings) {
        this.settings = settings;
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }
}
