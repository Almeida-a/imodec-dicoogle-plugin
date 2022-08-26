package pt.ua.imodec.util;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.imodec.util.validators.OSValidator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
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

        String tmpDicomFileName = String.format("/tmp/imodec/ImageUtils/%d.dcm", dicomObject.hashCode());
        File tmpDicomFile = new File(tmpDicomFileName);
        tmpDicomFile.deleteOnExit();

        if (!DicomUtils.saveDicomFile(dicomObject, tmpDicomFile, true)) {
            throw new FileAlreadyExistsException("Hash collision in creating the tmp file");
        }

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

    public static DicomObject encodeDicomObject(DicomObject dicomObject, NewFormat chosenFormat) throws IOException {

        logger.info("Encoding with recent formats...");

        if (!OSValidator.validate())
            throw new IllegalStateException(
                    String.format("Unsupported OS: '%s' for Imodec storage plugin", System.getProperty("os.name"))
            );

        // Change dicom object from uncompressed to JPEG XL, WebP or AVIF format
        BufferedImage dicomImage = loadDicomImage(dicomObject);

        String tmpFileName = String.format("/tmp/imodec/ImageUtils/%d.png", dicomImage.hashCode());
        File tmpImageFile = new File(tmpFileName);
        tmpImageFile.deleteOnExit();

        ImageIO.write(dicomImage, "png", tmpImageFile);

        byte[] bitstream = NewFormatsCodecs.encodePNGFile(tmpImageFile, chosenFormat);

        // Adding the new data into the dicom object
        dicomObject.putBytes(Tag.PixelData, VR.OB, bitstream);
        // TODO change parameters other than pixel-data (lossy compression, transfer syntax, ...)

        return dicomObject;
    }
}
