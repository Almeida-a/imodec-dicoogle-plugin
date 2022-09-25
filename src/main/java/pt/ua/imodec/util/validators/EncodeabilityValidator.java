package pt.ua.imodec.util.validators;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class EncodeabilityValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(EncodeabilityValidator.class);

    public static boolean validate(DicomInputStream dicomInputStream) throws IOException {
        return validate(dicomInputStream.readDicomObject());
    }

    public static boolean validate(DicomObject dicomObject) {
        if (dicomObject.contains(Tag.LossyImageCompression)
                && dicomObject.getString(Tag.LossyImageCompression).equals("01")) {
            logger.error("Lossy image compression has already been subjected, thus it cannot be re-applied. " +
                    "Aborting...");
            return false;
        }
        if (dicomObject.contains(Tag.AllowLossyCompression)
                && dicomObject.getString(Tag.AllowLossyCompression).equals("NO")) {
            logger.error("Lossy compression is not allowed to be applied in this dicom object " +
                    "(AllowLossyCompression field is set false)");
            return false;
        }

        return true;
    }

}
