package pt.ua.imodec.util;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputHandler;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.imodec.ImodecPluginSet;

import java.io.File;
import java.io.IOException;

public class DicomUtils {

    public static final Logger logger = LoggerFactory.getLogger(DicomUtils.class);

    /**
     * Saves dicom object to a file
     *
     * @param dicomObject dicom data to save
     */
    public static File saveDicomFile(DicomObject dicomObject, boolean temporary) throws IOException {

        String tmpDicomFileName = String.format("%s/DicomUtils/%s.dcm", ImodecPluginSet.TMP_DIR_PATH,
                dicomObject.getString(Tag.SOPInstanceUID));
        File dicomFile = new File(tmpDicomFileName);
        dicomFile.deleteOnExit();

        if (dicomFile.exists()) {
            logger.debug("File '" + dicomFile.getAbsolutePath() + "' already exists! No operation.");
            return dicomFile;
        }

        if (!MiscUtils.createNewFile(dicomFile, true))
            throw new IllegalStateException(
                    String.format("Could not create temporary file: '%s'", dicomFile)
            );

        if (temporary)
            dicomFile.deleteOnExit();

        try (DicomOutputStream outputStream = new DicomOutputStream(dicomFile)) {
            outputStream.writeDicomFile(dicomObject);
        }

        return dicomFile;

    }

    public static DicomObject readNonPixelData(DicomInputStream dicomInputStream) throws IOException {
        DicomInputHandler nonPixelDataHandler = new StopTagInputHandler(Tag.PixelData);

        dicomInputStream.setHandler(nonPixelDataHandler);

        return dicomInputStream.readDicomObject();
    }
}
