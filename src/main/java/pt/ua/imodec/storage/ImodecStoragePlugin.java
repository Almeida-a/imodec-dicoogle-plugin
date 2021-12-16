package pt.ua.imodec.storage;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.io.IOException;
import java.net.URI;

/**
 *
 * Basic Storage Plugin
 *  - "Template" from rlebre/dicoogle-plugin-sample
 *  - TODO add the compression features (
 *
 * */
public class ImodecStoragePlugin implements StorageInterface {
    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public Iterable<StorageInputStream> at(URI uri, Object... objects) {
        return null;
    }

    @Override
    public URI store(DicomObject dicomObject, Object... objects) {
        return null;
    }

    @Override
    public URI store(DicomInputStream dicomInputStream, Object... objects) throws IOException {
        return null;
    }

    @Override
    public void remove(URI uri) {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean enable() {
        return false;
    }

    @Override
    public boolean disable() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setSettings(ConfigurationHolder configurationHolder) {

    }

    @Override
    public ConfigurationHolder getSettings() {
        return null;
    }
}
