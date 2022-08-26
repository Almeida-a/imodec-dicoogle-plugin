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
     * @return Whether if the operation is successful or not
     */
    public static boolean saveDicomFile(DicomObject dicomObject, File dicomFile, boolean temporary) throws IOException {

        if (dicomFile.exists()) {
            logger.error("File '' " + dicomFile.getAbsolutePath() + "' already exists!");
            return false;
        }

        if (!MiscUtils.createNewFile(dicomFile))
            logger.warn(String.format("Could not create temporary file: '%s'", dicomFile));

        if (temporary)
            dicomFile.deleteOnExit();

        try (DicomOutputStream outputStream = new DicomOutputStream(dicomFile)) {
            outputStream.writeDicomFile(dicomObject);
            return true;
        }

    }

}
