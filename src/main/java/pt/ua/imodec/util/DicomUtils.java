package pt.ua.imodec.util;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class DicomUtils {

    public static final Logger logger = LoggerFactory.getLogger(DicomUtils.class);

    /**
     * Saves dicom object to a file
     *
     * @param dicomObject dicom data to save
     * @param dicomFile Path of the file that will be created
     */
    public static void saveDicomFile(DicomObject dicomObject, File dicomFile, boolean temporary) throws IOException {

        if (dicomFile.exists())
            logger.warn("File '" + dicomFile.getAbsolutePath() + "' already exists! Overwriting");

        if (!MiscUtils.createNewFile(dicomFile, true))
            throw new IllegalStateException(
                    String.format("Could not create temporary file: '%s'", dicomFile)
            );

        if (temporary)
            dicomFile.deleteOnExit();

        try (DicomOutputStream outputStream = new DicomOutputStream(dicomFile)) {
            outputStream.writeDicomFile(dicomObject);
        }

    }

}
