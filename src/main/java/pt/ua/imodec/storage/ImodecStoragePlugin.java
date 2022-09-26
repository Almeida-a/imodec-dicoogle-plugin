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
import pt.ua.imodec.util.DicomUtils;
import pt.ua.imodec.util.MiscUtils;
import pt.ua.imodec.datastructs.formats.Format;
import pt.ua.imodec.datastructs.formats.Native;
import pt.ua.imodec.datastructs.formats.NewFormat;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

/**
 *
 * Storage Plugin
 *  - "Template" from rlebre/dicoogle-plugin-sample
 * <p>
 * */
public class ImodecStoragePlugin implements StorageInterface {

    private static final Logger logger = LoggerFactory.getLogger(ImodecStoragePlugin.class);
    private static final String scheme = "imodec-mem";

    private final HashMap<String, ByteArrayOutputStream> mem = new HashMap<>();  // TODO: 16/09/22 Optimize: Refactor BAOS to OS, to allow for using other output streams - very good for memory optimization
    private boolean enabled = true;
    private ConfigurationHolder settings;

    @Override
    public String getScheme() {
        return scheme;
    }

    public boolean containsURI(final URI uri) {
        return mem.containsKey(uri.toString());
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
                public InputStream getInputStream() throws IOException {
                    ByteArrayOutputStream bos = mem.get(location.toString());

                    if (bos == null)
                        throw new NoSuchElementException(
                                String.format("File uri='%s' was not found at the storage!", location)
                        );

                    try {
                        return new ByteArrayInputStream(bos.toByteArray());
                    } catch (OutOfMemoryError ignored) {
                        logger.info("Large bitstream object encountered. " +
                                "Changing approach for data retrieval...");
                        return MiscUtils.getInputStreamFromLarge(bos);
                    }
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

        URI uri = getUri(dicomObject);
        if (mem.containsKey(uri.toString())) {
            logger.warn("This object was already stored!");
            return uri;
        }

        logger.info("Waiting while format is being set");
        Supplier<Boolean> choosingProcess = () -> ImodecPluginSet.chosenFormat == null;
        MiscUtils.sleepWhile(choosingProcess);
        Format chosenFormat = ImodecPluginSet.chosenFormat;
        boolean encodeWithAllTS = chosenFormat.getId().equals("all");
        boolean zerothLevelRecursion = objects.length == 0;


        try {
            if (chosenFormat instanceof NewFormat) {

                DicomUtils.encodeDicomObject(dicomObject, (NewFormat) chosenFormat, new HashMap<>());

            } else if (DicomUtils.isMultiFrame(dicomObject)
                    && encodeWithAllTS && zerothLevelRecursion) {

                logger.warn("This is not memory optimized. Memory errors are prone to occur.");
                File dicomObjectFile = DicomUtils.writeDicomObjectToTmpFile(dicomObject);
                Iterator<DicomInputStream> dicomInputStreamIterator = DicomUtils.encodeIteratorDicomInputStreamWithAllTs(dicomObjectFile);
                while (dicomInputStreamIterator.hasNext()) {
                    store(dicomInputStreamIterator.next(), Native.UNCHANGED);
                }

            } else if (encodeWithAllTS && zerothLevelRecursion) {  // Same as previous but single frame
                // TODO: 03/09/22 Find a better way for stopping condition than by the number of argument objects
                Iterator<DicomObject> dicomObjectsIterator = DicomUtils.encodeIteratorDicomObjectWithAllTs(dicomObject);
                while (dicomObjectsIterator.hasNext()) {
                    store(dicomObjectsIterator.next(), Native.UNCHANGED);
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
        mem.put(uri.toString(), bos);
        logger.info("Object successfully stored!");

        return uri;
    }

    private URI getUri(DicomObject dicomObject) {

        String tsUID;
        Format chosenFormat = ImodecPluginSet.chosenFormat;

        if (chosenFormat.equals(Native.UNCHANGED) || chosenFormat.getId().equals("all"))
            tsUID = dicomObject.getString(Tag.TransferSyntaxUID);
        else
            tsUID = chosenFormat.getTransferSyntax().uid();

        return URI.create(getScheme() + "://"
                + dicomObject.getString(Tag.SOPInstanceUID) + "/"
                + tsUID);
    }

    @Override
    public URI store(DicomInputStream dicomInputStream, Object... objects) throws IOException {
        return store(dicomInputStream.readDicomObject(), objects);
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
