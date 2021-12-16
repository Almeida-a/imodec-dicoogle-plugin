package pt.ua.imodec.storage;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

/**
 *
 * Basic Storage Plugin
 *  - "Template" from rlebre/dicoogle-plugin-sample
 *  - TODO add the compression features
 *
 * */
public class ImodecStoragePlugin implements StorageInterface {

    private static final Logger logger = LoggerFactory.getLogger(ImodecStoragePlugin.class);

    private final HashMap<String, ByteArrayOutputStream> mem = new HashMap<>();
    private boolean enabled = true;
    private ConfigurationHolder settings;

    @Override
    public String getScheme() {
        return "mem://";
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

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DicomOutputStream dos = new DicomOutputStream(bos);
        try {
            dos.writeDicomFile(dicomObject);
        } catch (IOException ex) {
            logger.warn("Failed to store object", ex);
        }
        bos.toByteArray();
        URI uri = URI.create("mem://" + UUID.randomUUID());
        mem.put(uri.toString(), bos);

        return uri;
    }

    @Override
    public URI store(DicomInputStream dicomInputStream, Object... objects) throws IOException {

        DicomObject obj = dicomInputStream.readDicomObject();

        return store(obj);
    }

    @Override
    public void remove(URI location) {
        this.mem.remove(location.toString());
    }

    @Override
    public String getName() {
        return "imodec-plugin-storage";
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
