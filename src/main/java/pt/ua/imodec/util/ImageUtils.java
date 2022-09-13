package pt.ua.imodec.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;
import org.dcm4che2.data.*;
import org.dcm4che2.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.imodec.ImodecPluginSet;
import pt.ua.imodec.util.formats.NewFormat;
import pt.ua.imodec.util.validators.EncodeabilityValidator;
import pt.ua.imodec.util.validators.OSValidator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.rmi.UnexpectedException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class ImageUtils {

    public static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

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
            ImageReader reader = getImageReader("DICOM");
            ImageReadParam param = reader.getDefaultReadParam();
            reader.setInput(imageInputStream, false);
            BufferedImage image = reader.read(frame, param);
            if (image == null)
                throw new NullPointerException("Error reading dicom image!");
            return image;
        }
    }

    public static BufferedImage loadDicomImage(DicomObject dicomObject, int frame) throws IOException {

        File tmpDicomFile = DicomUtils.saveDicomFile(dicomObject, true);

        DicomInputStream dicomInputStream = new DicomInputStream(tmpDicomFile);

        return loadDicomImage(dicomInputStream, frame);
    }

    public static Iterator<BufferedImage> loadDicomImageIterator(DicomInputStream dicomInputStream) throws IOException {

        File file = new File(ImodecPluginSet.TMP_DIR_PATH + "/loadIteratorTmp.dcm");
        file.deleteOnExit();
        FileUtils.copyInputStreamToFile(dicomInputStream, file);

        DicomObject meta = DicomUtils.readNonPixelData(new DicomInputStream(file));

        return new Iterator<BufferedImage>() {

            private int i = 0;
            private final int frames = meta.getInt(Tag.NumberOfFrames);
            @Override
            public boolean hasNext() {
                return i + 1 < frames;
            }

            @Override
            public BufferedImage next() {
                try {
                    return loadDicomImage(new DicomInputStream(file), i++);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static ImageReader getImageReader(String formatName) {

        Iterator<ImageReader> imageWriterIterator = ImageIO.getImageReadersByFormatName(formatName);

        if (!imageWriterIterator.hasNext())
            throw new NullPointerException(String.format("Format '%s' is not supported by ImageIO!", formatName));

        return imageWriterIterator.next();

    }

    public static ImageWriter getImageWriter(String formatName) {

        Iterator<ImageWriter> imageWriterIterator = ImageIO.getImageWritersByFormatName(formatName);

        if (!imageWriterIterator.hasNext())
            throw new NullPointerException(String.format("Format '%s' is not supported by ImageIO!", formatName));

        return imageWriterIterator.next();

    }

    public static ImageOutputStream getImageOutputStream(String output) throws IOException {
        File outfile = new File(output);
        return ImageIO.createImageOutputStream(outfile);
    }

    public static void encodeDicomObject(
            DicomObject dicomObject, NewFormat chosenFormat,
            HashMap<String, Number> options) throws IOException {

        if (dicomObject.getInt(Tag.NumberOfFrames) == 1)
            encodeSingleFrameDicomObject(dicomObject, chosenFormat, options);
        else
            encodeMultiFrameDicomObject(dicomObject, chosenFormat, options);

    }

    private static void encodeMultiFrameDicomObject(DicomObject dicomObject, NewFormat chosenFormat,
                                                    HashMap<String, Number> options) throws IOException {

        logger.info("Encoding multi-frame dicom object with '{}' format...",
                chosenFormat.getFileExtension());

        if (!EncodeabilityValidator.validate(dicomObject)) {
            logger.error("Cannot encode this dicom object!");
            throw new UnsupportedOperationException();
        }

        if (!OSValidator.validate())
            throw new IllegalStateException(
                    String.format("Unsupported OS: '%s' for Imodec storage plugin", System.getProperty("os.name"))
            );

        Iterator<BufferedImage> frameIterator = new FrameIterator(dicomObject);

        logger.debug("Creating directory to store the (multi-frame) image frames");
        String sopInstUID = dicomObject.getString(Tag.SOPInstanceUID);
        File imageDir = new File(String.format("%s/%s", ImodecPluginSet.TMP_DIR_PATH, sopInstUID));
        if (!imageDir.exists() && !imageDir.mkdirs()) {
            throw new UnexpectedException("Could not create directory!");
        }
        imageDir.deleteOnExit();

        int nativePixelDataSizeInBytes = dicomObject.getBytes(Tag.PixelData).length;

        while (frameIterator.hasNext()) {
            BufferedImage frame = frameIterator.next();

            logger.debug("Storing image frame into png file.");
            // TODO: 08/09/22 ID the frames by their order?
            File framePNG = new File(String.format("%s/%d.png", imageDir, frame.hashCode()));
            framePNG.deleteOnExit();
            ImageIO.write(frame, "png", framePNG);

            logger.debug("Adding frame bitstream into pixel data");
            NewFormatsCodecs.encodePNGFile(framePNG, chosenFormat, options);

        }

        logger.debug("Emptying pixel-data");
        dicomObject.putBytes(Tag.PixelData, VR.OB, new byte[]{-1});

        logger.debug("Inserting the encoded frames onto the pixel data sequence");
        DicomElement dicomFrames = dicomObject.putSequence(Tag.PixelData, dicomObject.getInt(Tag.NumberOfFrames));
        BasicDicomObject sequenceDicomObject = new BasicDicomObject();
        int compressedPixelDataSequenceSizeInBytes = 0;
        for (File encodedFrame : Objects.requireNonNull(imageDir.listFiles(file -> file.getName().endsWith(".png")))) {
            byte[] bitstream = Files.readAllBytes(encodedFrame.toPath());
            compressedPixelDataSequenceSizeInBytes += bitstream.length;
            sequenceDicomObject.putBytes(Tag.Item, VR.OB, bitstream);
            dicomFrames.addDicomObject(sequenceDicomObject);
        }

        // Change parameters other than pixel-data (lossy compression, transfer syntax, ...)
        updateLossyAttributes(dicomObject, chosenFormat, nativePixelDataSizeInBytes,
                compressedPixelDataSequenceSizeInBytes);
    }

    private static void updateLossyAttributes(DicomObject dicomObject, NewFormat chosenFormat,
                                              int nativePixelDataSizeInBytes, int compressedPixelDataSizeInBytes) {
        dicomObject.putString(Tag.TransferSyntaxUID, VR.UI, chosenFormat.getTransferSyntax().uid());
        dicomObject.putString(Tag.LossyImageCompression, VR.CS, "01");
        dicomObject.putString(Tag.LossyImageCompressionRatio, VR.DS,
                String.valueOf(nativePixelDataSizeInBytes / compressedPixelDataSizeInBytes));
        dicomObject.putString(Tag.LossyImageCompressionMethod, VR.CS, chosenFormat.getMethod());
    }

    private static void encodeSingleFrameDicomObject(DicomObject dicomObject, NewFormat chosenFormat, HashMap<String, Number> options) throws IOException {
        logger.info("Encoding single-frame dicom object with '{}' format...",
                chosenFormat.getFileExtension());

        if (!EncodeabilityValidator.validate(dicomObject)) {
            logger.error("Cannot encode this dicom object!");
            throw new UnsupportedOperationException();
        }

        if (!OSValidator.validate())
            throw new IllegalStateException(
                    String.format("Unsupported OS: '%s' for Imodec storage plugin", System.getProperty("os.name"))
            );

        int rawImageByteSize = dicomObject.getBytes(Tag.PixelData).length;

        // Change dicom object from uncompressed to JPEG XL, WebP or AVIF format
        BufferedImage dicomImage = loadDicomImage(dicomObject, 0);

        logger.debug("Storing image into png file.");
        File tmpImageFile = new File(String.format("%s/%d.png", ImodecPluginSet.TMP_DIR_PATH, dicomImage.hashCode()));
        tmpImageFile.deleteOnExit();
        ImageIO.write(dicomImage, "png", tmpImageFile);

        byte[] bitstream = NewFormatsCodecs.encodePNGFile(tmpImageFile, chosenFormat, options);
        int compressedImageByteSize = bitstream.length;

        // Adding the new data into the dicom object
        dicomObject.putBytes(Tag.PixelData, VR.OB, bitstream);
        // Change parameters other than pixel-data (lossy compression, transfer syntax, ...)
        updateLossyAttributes(dicomObject, chosenFormat, rawImageByteSize, compressedImageByteSize);
    }

    public static Iterator<DicomObject> encodeIteratorDicomObjectWithAllTs(DicomObject dicomObject) throws IOException {
        if (dicomObject.contains(Tag.LossyImageCompression)
                && dicomObject.getString(Tag.LossyImageCompression).equals("01"))
            throw new IllegalArgumentException("Cannot re-apply lossy compression to image!");

        Iterator<NewFormat> newFormats = Arrays.stream(NewFormat.values()).iterator();

        return new Iterator<DicomObject>() {
            @Override
            public boolean hasNext() {
                return newFormats.hasNext();
            }

            @Override
            public DicomObject next() {
                try {
                    DicomObject dicomObjectClone = (DicomObject) SerializationUtils.clone(dicomObject);
                    ImageUtils.encodeDicomObject(dicomObjectClone, newFormats.next(), new HashMap<>());
                    return dicomObjectClone;
                } catch (IOException e) {
                    logger.error("Unexpected error!");
                    throw new RuntimeException(e);
                } catch (OutOfMemoryError error) {
                    logger.error("File is too big!");
                    throw new RuntimeException(error);
                }
            }
        };
    }
}
