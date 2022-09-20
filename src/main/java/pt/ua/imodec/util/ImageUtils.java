package pt.ua.imodec.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;
import org.dcm4che2.data.*;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.imodec.ImodecPluginSet;
import pt.ua.imodec.util.formats.Format;
import pt.ua.imodec.util.formats.Native;
import pt.ua.imodec.util.formats.NewFormat;
import pt.ua.imodec.util.validators.EncodeabilityValidator;
import pt.ua.imodec.util.validators.OSValidator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.UnexpectedException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class ImageUtils {

    public static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    public static BufferedImage loadDicomImage(DicomObject dicomObject, int frame) throws IOException {

        File tmpDicomFile = DicomUtils.saveDicomFile(dicomObject, true);

        DicomInputStream dicomInputStream = new DicomInputStream(tmpDicomFile);

        return DicomUtils.loadDicomImage(dicomInputStream, frame);
    }

    public static Iterator<BufferedImage> loadDicomImageIterator(DicomInputStream dicomInputStream) throws IOException {

        File file = new File(ImodecPluginSet.TMP_DIR_PATH + "/loadIteratorTmp.dcm");
        file.deleteOnExit();
        FileUtils.copyInputStreamToFile(dicomInputStream, file);

        DicomObject meta = DicomUtils.readNonPixelData(new DicomInputStream(file));

        return new Iterator<BufferedImage>() {

            private int i = 0;
            private final int frames = meta.getInt(Tag.NumberOfFrames);
            private final String transferSyntax = meta.getString(Tag.TransferSyntaxUID);
            private final Format format = Arrays.stream((Format[]) NewFormat.values())
                    .filter(format -> format.getTransferSyntax().uid().equals(transferSyntax))
                    .findFirst()
                    .orElse(Native.UNCHANGED);

            @Override
            public boolean hasNext() {
                return i + 1 < frames;
            }

            @Override
            public BufferedImage next() {
                try {
                    if (format instanceof NewFormat) {
                        try (DicomInputStream inputStream = new DicomInputStream(file)) {
                            return DicomUtils.loadDicomEncodedFrame(inputStream, i, (NewFormat) format);
                        }
                    }
                    return DicomUtils.loadDicomImage(new DicomInputStream(file), i++);
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

    public static void encodeDicomObject(
            DicomObject dicomObject, NewFormat chosenFormat,
            HashMap<String, Number> options) throws IOException {

        if (DicomUtils.isMultiFrame(dicomObject))
            encodeMultiFrameDicomObject(dicomObject, chosenFormat, options);
        else
            encodeSingleFrameDicomObject(dicomObject, chosenFormat, options);

    }

    private static void encodeMultiFrameDicomObject(DicomObject dicomObject, NewFormat chosenFormat,
                                                    HashMap<String, Number> options) throws IOException {
        int framesSqLength = 0;

        logger.info("Encoding multi-frame dicom object with '{}' format. This might take a while...",
                chosenFormat.getFileExtension());

        if (!EncodeabilityValidator.validate(dicomObject)) {
            logger.error("Cannot encode this dicom object!");
            throw new UnsupportedOperationException();
        }

        if (!OSValidator.validate()) {
            throw new IllegalStateException(
                    String.format("Unsupported OS: '%s' for Imodec storage plugin", System.getProperty("os.name"))
            );
        }

        Iterator<BufferedImage> frameIterator = new FrameIterator(dicomObject);

        logger.debug("Creating directory to store the (multi-frame) image frames");
        String sopInstUID = dicomObject.getString(Tag.SOPInstanceUID);
        File imageDir = new File(String.format("%s/%s", ImodecPluginSet.TMP_DIR_PATH, sopInstUID));
        if (!imageDir.exists() && !imageDir.mkdirs()) {
            throw new UnexpectedException("Could not create directory!");
        }
        imageDir.deleteOnExit();

        int nativePixelDataSizeInBytes;
        DicomElement pixelDataElement = dicomObject.get(Tag.PixelData);
        if (pixelDataElement.vr().equals(VR.SQ))
            nativePixelDataSizeInBytes = pixelDataElement.getBytes().length;
        else
            nativePixelDataSizeInBytes = pixelDataElement.length();

        int frameCounter = 0;

        while (frameIterator.hasNext()) {
            BufferedImage frame = frameIterator.next();

            logger.debug("Storing image frame into png file.");
            File framePNG = new File(String.format("%s/%d.png", imageDir, frameCounter++));
            framePNG.deleteOnExit();
            ImageIO.write(frame, "png", framePNG);

            logger.debug("Adding frame bitstream into pixel data");
            byte[] codeStream = NewFormatsCodecs.encodePNGFile(framePNG, chosenFormat, options);
            framesSqLength += codeStream.length;

        }

        TransferSyntax dicomObjectsTS = TransferSyntax.valueOf(dicomObject.getString(Tag.TransferSyntaxUID));

        logger.debug("Emptying pixel-data (re-writing with the sum of the length of all frames or -1).");
        DicomElement dicomFramesSequence;
        if (dicomObjectsTS.explicitVR())
            dicomFramesSequence = dicomObject.putSequence(Tag.PixelData, framesSqLength);
        else
            dicomFramesSequence = dicomObject.putSequence(Tag.PixelData);

        logger.debug("Inserting the encoded frames onto the pixel data sequence");
        BasicDicomObject sequenceDicomObject = new BasicDicomObject();
        for (File encodedFrame : Objects.requireNonNull(imageDir.listFiles(file -> file.getName().endsWith(".png")))) {
            byte[] bitstream = Files.readAllBytes(encodedFrame.toPath());
            sequenceDicomObject.putBytes(Tag.Item, VR.OB, bitstream);
        }
        if (!dicomObjectsTS.explicitVR())
            sequenceDicomObject.putNull(Tag.SequenceDelimitationItem, VR.OB);
        dicomFramesSequence.addDicomObject(sequenceDicomObject);

        logger.debug("Changing parameters other than pixel-data (lossy compression, transfer syntax, ...)");
        int compressedPixelDataSequenceSizeInBytes = framesSqLength;
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

    public static Iterator<DicomObject> encodeIteratorDicomObjectWithAllTs(DicomObject dicomObject) {
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

    /**
     *
     * @param dicomObjectFile Assumed to be multi-frame
     * @return Iterator with input stream objects
     */
    public static Iterator<DicomInputStream> encodeIteratorDicomInputStreamWithAllTs(File dicomObjectFile) {

        Iterator<NewFormat> newFormatIterator = Arrays.stream(NewFormat.values()).iterator();

        return new Iterator<DicomInputStream>() {
            @Override
            public boolean hasNext() {
                return newFormatIterator.hasNext();
            }

            @Override
            public DicomInputStream next() {
                try {
                    logger.debug("Fetching dicom object from file");
                    DicomInputStream dicomInputStream = new DicomInputStream(dicomObjectFile);
                    DicomObject dicomObject = dicomInputStream.readDicomObject();  // FIXME: 20/09/22 OOM Hazard
                    dicomInputStream.close();

                    ImageUtils.encodeMultiFrameDicomObject(dicomObject, newFormatIterator.next(), new HashMap<>());
                    File file1 = writeDicomObjectToTmpFile(dicomObject);
                    return new DicomInputStream(file1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (OutOfMemoryError error) {
                    logger.error("Multi-frame dicom object is too big!");
                    throw new RuntimeException(error);
                }
            }
        };
    }

    public static File writeDicomObjectToTmpFile(DicomObject dicomObject) throws IOException {
        String tmpFileName = String.format("%s/tmp%s-%s.dcm", ImodecPluginSet.TMP_DIR_PATH,
                dicomObject.getString(Tag.SOPInstanceUID), dicomObject.getString(Tag.TransferSyntaxUID));

        File file = new File(tmpFileName);
        if (!file.exists()) {
            boolean ignored = file.createNewFile();

            logger.debug("Writing dicom object to file");
            DicomOutputStream dicomOutputStream = new DicomOutputStream(file);
            dicomOutputStream.writeDicomFile(dicomObject);
            dicomOutputStream.close();
        }
        file.deleteOnExit();
        return file;
    }
}
