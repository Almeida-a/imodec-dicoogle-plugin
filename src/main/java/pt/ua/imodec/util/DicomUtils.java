package pt.ua.imodec.util;

import org.dcm4che2.data.*;
import org.dcm4che2.io.DicomInputHandler;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.imodec.ImodecPluginSet;
import pt.ua.imodec.util.formats.NewFormat;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

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

    public static boolean isMultiFrame(DicomObject dicomObject) {
        return dicomObject.contains(Tag.NumberOfFrames) && dicomObject.getInt(Tag.NumberOfFrames) > 1;
    }

    static BufferedImage loadDicomEncodedFrame(DicomInputStream inputStream, int frameID, NewFormat newFormat) throws IOException {
        // FIXME: 17/09/22 This readDicomObject is an OOM hazard.
        //  Use DicomInputStream to read the frames w/o loading them all to memory

        DicomObject dicomObject = inputStream.readDicomObject();
        DicomElement frameSequence = dicomObject.get(Tag.PixelData);

        if (frameSequence.vr().equals(VR.SQ))
            throw new AssertionError("Tried to load a frame from a non multi-frame dicom object!");

        byte[] codeStream = frameSequence.getFragment(frameID);

        Optional<BufferedImage> image = Optional.ofNullable(
                NewFormatsCodecs.decodeByteStream(codeStream, newFormat));
        if (!image.isPresent())
            throw new NullPointerException("Error reading frame!");
        return image.get();
    }

    /**
     * Load dicom (buffered) image.
     * Credits:
     * <a href="https://github.com/bioinformatics-ua/dicoogle/blob/0a5ab168a2c96dd3637c6fa222cdf04323a473ce/dicoogle
     * /src/main/java/pt/ua/dicoogle/server/web/utils/ImageLoader.java#L97-L106">Source</a>.
     *
     * @param inputStream Stream with the dicom data
     * @param frame ordinal value of the frame to be retrieved, if image is single frame, then always 0
     * @return The buffered image
     * @throws IOException if the image format is not DICOM or another IO issue occurred
     */
    public static BufferedImage loadDicomImage(DicomInputStream inputStream, int frame) throws IOException {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            ImageReader reader = ImageUtils.getImageReader("DICOM");
            ImageReadParam param = reader.getDefaultReadParam();
            reader.setInput(imageInputStream, false);
            BufferedImage image = reader.read(frame, param);
            if (image == null)
                throw new NullPointerException("Error reading dicom image!");
            return image;
        }
    }
}
