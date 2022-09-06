package pt.ua.imodec.util;

import org.apache.commons.lang.SerializationUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.imodec.ImodecPluginSet;
import pt.ua.imodec.util.formats.NewFormat;
import pt.ua.imodec.util.validators.OSValidator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class ImageUtils {

    public static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    /**
     *
     * Load dicom (buffered) image.
     * Credits:
     *  <a href="https://github.com/bioinformatics-ua/dicoogle/blob/0a5ab168a2c96dd3637c6fa222cdf04323a473ce/dicoogle
     *  /src/main/java/pt/ua/dicoogle/server/web/utils/ImageLoader.java#L97-L106">Source</a>.
     *
     * @param inputStream Stream with the dicom data
     * @return The buffered image
     * @throws IOException if the image format is not DICOM or another IO issue occurred
     */
    public static BufferedImage loadDicomImage(DicomInputStream inputStream) throws IOException {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            ImageReader reader = getImageReader("DICOM");
            ImageReadParam param = reader.getDefaultReadParam();
            reader.setInput(imageInputStream, false);
            BufferedImage image = reader.read(0, param);
            if (image == null)
                throw new NullPointerException("Error reading dicom image!");
            return image;
        }
    }

    public static BufferedImage loadDicomImage(DicomObject dicomObject) throws IOException {

        String tmpDicomFileName = String.format("%s/ImageUtils/%s.dcm", ImodecPluginSet.tmpDirPath,
                dicomObject.getString(Tag.SOPInstanceUID));
        File tmpDicomFile = new File(tmpDicomFileName);
        tmpDicomFile.deleteOnExit();

        DicomUtils.saveDicomFile(dicomObject, tmpDicomFile, true);

        DicomInputStream dicomInputStream = new DicomInputStream(tmpDicomFile);

        return loadDicomImage(dicomInputStream);
    }

    public static ImageWriter getImageWriter(String formatName) {

        Iterator<ImageWriter> imageWriterIterator = ImageIO.getImageWritersByFormatName(formatName);

        if (!imageWriterIterator.hasNext())
            throw new NullPointerException(String.format("Format '%s' is not supported by ImageIO!", formatName));

        return imageWriterIterator.next();

    }

    public static ImageReader getImageReader(String formatName) {

        Iterator<ImageReader> imageWriterIterator = ImageIO.getImageReadersByFormatName(formatName);

        if (!imageWriterIterator.hasNext())
            throw new NullPointerException(String.format("Format '%s' is not supported by ImageIO!", formatName));

        return imageWriterIterator.next();

    }

    public static void encodeDicomObject(DicomObject dicomObject, NewFormat chosenFormat,
                                         HashMap<String, Number> options) throws IOException {

        logger.info("Encoding with recent formats...");

        if (dicomObject.contains(Tag.LossyImageCompression)
                && dicomObject.getString(Tag.LossyImageCompression).equals("01")) {
            logger.error("Lossy image compression has already been subjected, thus it cannot be re-applied. " +
                    "Aborting...");
            return;
        }
        if (dicomObject.contains(Tag.AllowLossyCompression)
                && dicomObject.getString(Tag.AllowLossyCompression).equals("NO")) {
            logger.error("Lossy compression is not allowed to be applied in this dicom object " +
                    "(AllowLossyCompression field is set false)");
            return;
        }

        if (!OSValidator.validate())
            throw new IllegalStateException(
                    String.format("Unsupported OS: '%s' for Imodec storage plugin", System.getProperty("os.name"))
            );

        int rawImageByteSize = dicomObject.getBytes(Tag.PixelData).length;

        // Change dicom object from uncompressed to JPEG XL, WebP or AVIF format
        BufferedImage dicomImage = loadDicomImage(dicomObject);

        String tmpFileName = String.format("%s/ImageUtils/%d.png", ImodecPluginSet.tmpDirPath, dicomImage.hashCode());
        File tmpImageFile = new File(tmpFileName);
        tmpImageFile.deleteOnExit();

        ImageIO.write(dicomImage, "png", tmpImageFile);

        byte[] bitstream = NewFormatsCodecs.encodePNGFile(tmpImageFile, chosenFormat, options);
        int compressedImageByteSize = bitstream.length;

        // Adding the new data into the dicom object
        dicomObject.putBytes(Tag.PixelData, VR.OB, bitstream);
        // Change parameters other than pixel-data (lossy compression, transfer syntax, ...)
        dicomObject.putString(Tag.TransferSyntaxUID, VR.UI, chosenFormat.getTransferSyntax().uid());
        dicomObject.putString(Tag.LossyImageCompression, VR.CS, "01");
        dicomObject.putString(Tag.LossyImageCompressionRatio, VR.DS, String.valueOf(rawImageByteSize / compressedImageByteSize));
        dicomObject.putString(Tag.LossyImageCompressionMethod, VR.CS, chosenFormat.getMethod());

    }

    public static DicomObject[] encodeDicomObjectWithAllTs(DicomObject dicomObject) throws IOException {

        if (dicomObject.contains(Tag.LossyImageCompression)
                && dicomObject.getString(Tag.LossyImageCompression).equals("01"))
            throw new IllegalArgumentException("Cannot re-apply lossy compression to image!");

        NewFormat[] newFormats = NewFormat.values();
        DicomObject[] clones = new DicomObject[newFormats.length + 1];

        // The "original" clone - like Boba Fett to the clone army
        clones[0] = dicomObject;

        for (int i = 0; i < newFormats.length; i++) {
            clones[i+1] = (DicomObject) SerializationUtils.clone(dicomObject);
            ImageUtils.encodeDicomObject(clones[i+1], newFormats[i], new HashMap<>());
        }

        return clones;

    }
}
