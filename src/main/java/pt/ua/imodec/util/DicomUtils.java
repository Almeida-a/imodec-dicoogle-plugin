package pt.ua.imodec.util;

import org.apache.commons.lang.SerializationUtils;
import org.dcm4che2.data.*;
import org.dcm4che2.io.DicomInputHandler;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.imodec.ImodecPluginSet;
import pt.ua.imodec.datastructs.FrameIterator;
import pt.ua.imodec.datastructs.formats.NewFormat;
import pt.ua.imodec.util.validators.EncodeabilityValidator;
import pt.ua.imodec.util.validators.OSValidator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.*;

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

    public static boolean isMultiFrame(File dicomFile) throws IOException {
        try (DicomInputStream dicomInputStream = new DicomInputStream(dicomFile)) {
            try {
                DicomObject dicomObject = dicomInputStream.readDicomObject();
                return isMultiFrame(dicomObject);
            } catch (OutOfMemoryError ignored) {
                DicomInputStream dicomInputStream2 = new DicomInputStream(dicomFile);
                dicomInputStream2.setHandler(new StopTagInputHandler(Tag.PixelData));
                DicomObject dicomObject = dicomInputStream2.readDicomObject();

                boolean hasMultipleFrames = dicomObject.getInt(Tag.NumberOfFrames) > 1;
                dicomInputStream2.close();
                return hasMultipleFrames;
            }
        }
    }

    public static boolean isMultiFrame(DicomObject dicomObject) {

        boolean areThereMultipleNumberOfFrames = dicomObject.contains(Tag.NumberOfFrames) && dicomObject.getInt(Tag.NumberOfFrames) > 1;
        boolean isVrSq = dicomObject.get(Tag.PixelData).vr().equals(VR.SQ);
        boolean areThereMultiplePixelDataItems = dicomObject.get(Tag.PixelData).countItems() > 1;

        if (!(areThereMultipleNumberOfFrames == areThereMultiplePixelDataItems == isVrSq))
            logger.warn("This is an inconsistent dicom object regarding the frames!");

        return areThereMultipleNumberOfFrames || isVrSq || areThereMultiplePixelDataItems;
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

    public static void encodeDicomObject(
            DicomObject dicomObject, NewFormat chosenFormat,
            HashMap<String, Number> options) throws IOException {

        if (isMultiFrame(dicomObject))
            encodeMultiFrameDicomObject(dicomObject, chosenFormat, options);
        else
            encodeSingleFrameDicomObject(dicomObject, chosenFormat, options);

    }

    static void encodeMultiFrameDicomObject(DicomObject dicomObject, NewFormat chosenFormat,
                                            HashMap<String, Number> options) throws IOException {
        int framesSqLength = 0;

        ImageUtils.logger.info("Encoding multi-frame dicom object with '{}' format. This might take a while...",
                chosenFormat.getFileExtension());

        if (!EncodeabilityValidator.validate(dicomObject)) {
            ImageUtils.logger.error("Cannot encode this dicom object!");
            throw new UnsupportedOperationException();
        }

        if (!OSValidator.validate()) {
            throw new IllegalStateException(
                    String.format("Unsupported OS: '%s' for Imodec storage plugin", System.getProperty("os.name"))
            );
        }

        Iterator<BufferedImage> frameIterator = new FrameIterator(dicomObject);

        ImageUtils.logger.debug("Creating directory to store the (multi-frame) image frames");
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

        TransferSyntax dicomObjectsTS = TransferSyntax.valueOf(dicomObject.getString(Tag.TransferSyntaxUID));

        Queue<byte[]> codeStreamQueue = new LinkedList<>();

        while (frameIterator.hasNext()) {
            BufferedImage frame = frameIterator.next();

            ImageUtils.logger.debug("Storing image frame into png file.");
            File framePNG = new File(String.format("%s/%d.png", imageDir, frameCounter++));
            framePNG.deleteOnExit();
            ImageIO.write(frame, "png", framePNG);

            ImageUtils.logger.debug("Adding frame bitstream into pixel data");
            byte[] codeStream = NewFormatsCodecs.encodePNGFile(framePNG, chosenFormat, options);
            framesSqLength += codeStream.length;

            codeStreamQueue.add(codeStream);
        }

        ImageUtils.logger.debug("Emptying pixel-data (re-writing with the sum of the length of all frames or -1).");
        DicomElement dicomFramesSequence;
        if (dicomObjectsTS.explicitVR())
            dicomFramesSequence = dicomObject.putSequence(Tag.PixelData, framesSqLength);
        else
            dicomFramesSequence = dicomObject.putSequence(Tag.PixelData);

        BasicDicomObject pixelDataItem = new BasicDicomObject();

        ImageUtils.logger.debug("Inserting the encoded frames onto the pixel data sequence");

        while (!codeStreamQueue.isEmpty()) {
            pixelDataItem.putBytes(Tag.Item, VR.OB, codeStreamQueue.poll());
            dicomFramesSequence.addDicomObject(pixelDataItem);
            pixelDataItem = new BasicDicomObject();
        }
        if (!dicomObjectsTS.explicitVR())
            pixelDataItem.putNull(Tag.SequenceDelimitationItem, VR.OB);

        ImageUtils.logger.debug("Changing parameters other than pixel-data (lossy compression, transfer syntax, ...)");
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
        ImageUtils.logger.info("Encoding single-frame dicom object with '{}' format...",
                chosenFormat.getFileExtension());

        if (!EncodeabilityValidator.validate(dicomObject)) {
            ImageUtils.logger.error("Cannot encode this dicom object!");
            throw new UnsupportedOperationException();
        }

        if (!OSValidator.validate())
            throw new IllegalStateException(
                    String.format("Unsupported OS: '%s' for Imodec storage plugin", System.getProperty("os.name"))
            );

        int rawImageByteSize = dicomObject.get(Tag.PixelData).length();
        if (rawImageByteSize == -1) {
            logger.error("Invalid pixel data for a single frame dicom object!");
            throw new IllegalArgumentException();
        }

        // Change dicom object from uncompressed to JPEG XL, WebP or AVIF format
        BufferedImage dicomImage = ImageUtils.loadDicomImage(dicomObject, 0);

        ImageUtils.logger.debug("Storing image into png file.");
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

    /**
     * Encodes the dicom object with each format specified in datastructs.formats.NewFormats
     *
     * @param dicomObject Source data
     * @return Iterator with each encoded form
     */
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
                    encodeDicomObject(dicomObjectClone, newFormats.next(), new HashMap<>());
                    return dicomObjectClone;
                } catch (IOException e) {
                    ImageUtils.logger.error("Unexpected error!");
                    throw new RuntimeException(e);
                } catch (OutOfMemoryError error) {
                    ImageUtils.logger.error("File is too big!");
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
                    ImageUtils.logger.debug("Fetching dicom object from file");
                    DicomInputStream dicomInputStream = new DicomInputStream(dicomObjectFile);
                    DicomObject dicomObject = dicomInputStream.readDicomObject();  // FIXME: 20/09/22 OOM Hazard
                    dicomInputStream.close();

                    if (isMultiFrame(dicomObject))
                        encodeMultiFrameDicomObject(dicomObject, newFormatIterator.next(), new HashMap<>());
                    else
                        encodeSingleFrameDicomObject(dicomObject, newFormatIterator.next(), new HashMap<>());
                    File file1 = writeDicomObjectToTmpFile(dicomObject);
                    return new DicomInputStream(file1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (OutOfMemoryError error) {
                    ImageUtils.logger.error("Multi-frame dicom object is too big!");
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

            ImageUtils.logger.debug("Writing dicom object to file");
            DicomOutputStream dicomOutputStream = new DicomOutputStream(file);
            dicomOutputStream.writeDicomFile(dicomObject);
            dicomOutputStream.close();
        }
        file.deleteOnExit();
        return file;
    }
}
