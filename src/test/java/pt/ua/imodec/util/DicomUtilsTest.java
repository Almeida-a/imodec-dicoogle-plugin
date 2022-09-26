package pt.ua.imodec.util;

import org.apache.commons.io.FileUtils;
import org.dcm4che2.data.ConfigurationError;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.imageio.ImageReaderFactory;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.ua.imodec.datastructs.MergeIterator;
import pt.ua.imodec.datastructs.formats.NewFormat;
import pt.ua.imodec.util.validators.EncodeabilityValidator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DicomUtilsTest {

    public static final String DICOM_DATASET_DIR = "/home/archy/ic-encoders-eval/images/dataset_dicom/";

    /**
     * Overload of isReadable(DicomObject).
     * Also checks if the dicom object can be read from the dicomInputStream without triggering OutOfMemoryError
     * @param dicomFile evaluated data source
     * @return True If the dicom file can be read, as well as its image. False otherwise.
     * @throws IOException If an IO Error occurs
     */
    static boolean isReadable(File dicomFile) throws IOException {
        try (DicomInputStream dicomInputStream = new DicomInputStream(dicomFile)) {

            DicomObject dicomObject = dicomInputStream.readDicomObject();

            String transferSyntaxUid = dicomObject.getString(Tag.TransferSyntaxUID);

            return supportedTS(transferSyntaxUid);
        } catch (OutOfMemoryError ignored) {
            return false;
        }

    }

    /**
     * Checks if there's an available reader that can decode the pixel-data
     * @param transferSyntax associated to the pixel data
     * @return true if a reader exists, false otherwise
     */
    static boolean supportedTS(String transferSyntax) {
        ImageReaderFactory imageReaderFactory = ImageReaderFactory.getInstance();
        try {
            ImageReader ignored = imageReaderFactory.getReaderForTransferSyntax(transferSyntax);
        } catch (ConfigurationError ignored) {
            return false;
        } catch (UnsupportedOperationException ignored) {
            return true;
        }
        return true;
    }

    /**
     * Overload of getDicomDatasetFiles(String, int), with a default limit
     * @param datasetDir Directory containing the dicom files
     * @return Iterator containing references to dicom files
     */
    static Iterator<File> getDicomDatasetFiles(String datasetDir) {
        return getDicomDatasetFiles(datasetDir, 4, true, true);
    }

    /**
     * Provides a set of dicom file references inside the provided dataset directory
     *
     * @param datasetDir     Directory containing the dicom files
     * @param limit          Number of files provided
     * @param isSingleFrame  True if and only if the dicom object has only single frame
     * @param noLossyApplied If true, the iterator won't include lossy compressed dicom files
     * @return Iterator containing references of dicom files
     */
    static Iterator<File> getDicomDatasetFiles(String datasetDir, int limit, boolean isSingleFrame, boolean noLossyApplied) {

        File datasetDirFile = tryToFindDatasetDir(datasetDir);

        FileFilter fileFilter = (File dicomFile) -> tryLoadDicom(dicomFile).isPresent()
                && isSingleFrame != assertDoesNotThrow(() -> DicomUtils.isMultiFrame(dicomFile))
                && assertDoesNotThrow(() -> isReadable(dicomFile))
                && (!noLossyApplied ||
                assertDoesNotThrow(() -> EncodeabilityValidator.validate(new DicomInputStream(dicomFile))));

        return new Iterator<File>() {

            private final Iterator<File> fileIterator = Arrays.stream(
                    Objects.requireNonNull(datasetDirFile.listFiles()))
                    .filter(fileFilter::accept)
                    .iterator();

            private int i = 0;

            @Override
            public boolean hasNext() {
                return fileIterator.hasNext() && i++ < limit;
            }

            @Override
            public File next() {
                return assertDoesNotThrow(fileIterator::next);
            }
        };
    }

    private static Optional<DicomInputStream> tryLoadDicom(File dicomFile) {
        try {
            DicomInputStream dicomInputStream = new DicomInputStream(dicomFile);
            return Optional.of(dicomInputStream);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static File tryToFindDatasetDir(String datasetDir) {
        Path datasetDirPath = Paths.get(datasetDir);
        File datasetDirFile = datasetDirPath.toFile();
        Assumptions.assumeTrue(datasetDirFile.exists() && datasetDirFile.isDirectory());
        return datasetDirFile;
    }

    /**
     * Checks if transfer syntax has an associated image reader that can read the dicom
     *  object's image
     * @param dicomObject data source
     * @return True if there is a reader, false otherwise
     */
    static boolean isReadable(DicomObject dicomObject) {
        return supportedTS(dicomObject.getString(Tag.TransferSyntaxUID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/archy/ic-encoders-eval/images/dataset_dicom/"})
    void saveDicomFile(String datasetDir) {

        Iterator<File> dicomIterator = getDicomDatasetFiles(datasetDir);

        while (dicomIterator.hasNext()) {
            File dicomFile = dicomIterator.next();

            DicomInputStream dicomInputStream = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));
            try {
                DicomObject dicomObject = assertDoesNotThrow(() -> dicomInputStream.readDicomObject());

                File savedDicomFile = assertDoesNotThrow(() -> DicomUtils.saveDicomFile(dicomObject, false));

                DicomInputStream dicomInputStream2 = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));
                int stream2Size = assertDoesNotThrow(dicomInputStream2::available);
                byte[] dicomBytes2 = new byte[stream2Size];
                assertDoesNotThrow(() -> dicomInputStream2.read(dicomBytes2));

                DicomInputStream dicomInputStream3 = assertDoesNotThrow(() -> new DicomInputStream(savedDicomFile));
                int stream3Size = assertDoesNotThrow(dicomInputStream3::available);
                byte[] dicomBytes3 = new byte[stream3Size];
                assertDoesNotThrow(() -> dicomInputStream3.read(dicomBytes3));

                assertArrayEquals(dicomBytes2, dicomBytes3);

                assertTrue(savedDicomFile.delete());
            } catch (OutOfMemoryError error) {
                System.out.println("Too big dicom file to be read into a DicomObject! Will skip.");
            }
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/archy/ic-encoders-eval/images/dataset_dicom/"})
    void readNonPixelData(String datasetDir) {

        Iterator<File> dicomIterator = getDicomDatasetFiles(datasetDir);

        while (dicomIterator.hasNext()) {
            File dicomFile = dicomIterator.next();

            DicomInputStream dicomInputStream = assertDoesNotThrow(
                    () -> new DicomInputStream(dicomFile));
            DicomInputStream dicomInputStream2 = assertDoesNotThrow(
                    () -> new DicomInputStream(dicomFile));

            DicomObject dicomObject = assertDoesNotThrow(() -> DicomUtils.readNonPixelData(dicomInputStream));

            dicomInputStream2.setHandler(new StopTagInputHandler(Tag.PixelData));
            DicomObject dicomObject2 = assertDoesNotThrow(() -> dicomInputStream2.readDicomObject());

            assertEquals(dicomObject, dicomObject2);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/archy/ic-encoders-eval/images/dataset_dicom/"})
    @Disabled
    void loadDicomEncodedFrame(String datasetDir) {

        Iterator<File> dicomIterator = getDicomDatasetFiles(datasetDir);

        while (dicomIterator.hasNext()) {

            File dicomFile = dicomIterator.next();

            DicomInputStream dicomInputStream = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));

            // TODO: 23/09/22 Continue here...
//            byte[] codeStream1 = DicomUtils.loadDicomEncodedFrame();

        }

        fail("Test not implemented!");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/archy/ic-encoders-eval/images/dataset_dicom/"})
    void loadDicomImage(String datasetDir) {

        Iterator<File> dicomIterator = getDicomDatasetFiles(datasetDir);

        while (dicomIterator.hasNext()) {
            File dicomFile = dicomIterator.next();

            try {
                DicomInputStream dicomInputStream = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));
                DicomObject dicomObject = assertDoesNotThrow(() -> dicomInputStream.readDicomObject());
                if (!isReadable(dicomObject)) {
                    System.out.println("Dicom Object's image format is not supported! Will skip.");
                    continue;
                }
            } catch (OutOfMemoryError ignored) {
                System.out.println("Too big a dicom file! Will skip.");
                continue;
            }

            DicomInputStream dicomInputStream1 = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));
            BufferedImage actualImage = assertDoesNotThrow(
                    () -> DicomUtils.loadDicomImage(dicomInputStream1, 0));

            ImageReader dicomImageReader = ImageUtils.getImageReader("DICOM");
            DicomInputStream dicomInputStream2 = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));
            ImageReadParam defaultReadParam = dicomImageReader.getDefaultReadParam();
            ImageInputStream imageInputStream = assertDoesNotThrow(
                    () -> ImageIO.createImageInputStream(dicomInputStream2));
            assertDoesNotThrow(() -> dicomImageReader.setInput(imageInputStream));
            BufferedImage expectedImage = assertDoesNotThrow(
                    () -> dicomImageReader.read(0, defaultReadParam));

            boolean equality = assertDoesNotThrow(
                    () -> ImageUtilsTest.compare(actualImage, expectedImage));
            assertTrue(equality);
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/archy/ic-encoders-eval/images/dataset_dicom/"})
    void encodeSingleFrameDicomObject(String datasetDir) {

        Iterator<File> dicomIterator = getDicomDatasetFiles(datasetDir);

        while (dicomIterator.hasNext()) {
            File dicomFile = dicomIterator.next();

            try {
                DicomInputStream ogDicomInputStream = new DicomInputStream(dicomFile);
                DicomInputStream dicomInputStream = new DicomInputStream(dicomFile);

                DicomObject dicomObjectJxl = dicomInputStream.readDicomObject();

                if (DicomUtils.isMultiFrame(dicomObjectJxl)) {
                    System.out.println("Skipping multi-frame dicom object.");
                    continue;
                }

                HashMap<String, Number> options = new HashMap<>();
                options.put("distance", 0.0);
                DicomUtils.encodeDicomObject(dicomObjectJxl, NewFormat.JPEG_XL, options);

                BufferedImage expectedImage = DicomUtils.loadDicomImage(ogDicomInputStream, 0);

                byte[] codeStream = dicomObjectJxl.getBytes(Tag.PixelData);
                BufferedImage actualDecodedImage = NewFormatsCodecs.decodeByteStream(codeStream, NewFormat.JPEG_XL);

                boolean equality = ImageUtilsTest.compare(expectedImage, actualDecodedImage);

                assertTrue(equality);

                ogDicomInputStream.close();
                dicomInputStream.close();
            } catch (OutOfMemoryError ignored) {
                System.out.println("Dicom file is too big to be read to a DicomObject! Will skip.");
            } catch (IOException ignored) {
                fail();
            }

        }

    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/archy/ic-encoders-eval/images/dataset_dicom/"})
    void encodeMultiFrameDicomObject(String datasetDir) {

        Iterator<File> dicomIterator = getDicomDatasetFiles(datasetDir, 2, false, true);

        while (dicomIterator.hasNext()) {
            File dicomFile = dicomIterator.next();

            try {
                DicomInputStream dicomInputStream = new DicomInputStream(dicomFile);
                DicomObject dicomObject = dicomInputStream.readDicomObject();

                HashMap<String, Number> options = new HashMap<>();
                options.put("distance", 0.0);
                assertDoesNotThrow(
                        () -> DicomUtils.encodeMultiFrameDicomObject(dicomObject, NewFormat.JPEG_XL, options));

                int numberOfFrames = dicomObject.getInt(Tag.NumberOfFrames);
                DicomElement pixelDataElement = dicomObject.get(Tag.PixelData);
                for (int i = 0; i < numberOfFrames; i++) {
                    byte[] frameCodeStream = pixelDataElement.getDicomObject(i).getBytes(Tag.Item);
                    BufferedImage decodedImage = NewFormatsCodecs.decodeByteStream(frameCodeStream, NewFormat.JPEG_XL);
                    BufferedImage originalImage = DicomUtils.loadDicomImage(new DicomInputStream(dicomFile), i);

                    boolean imageEquality = ImageUtilsTest.compare(decodedImage, originalImage);
                    assertTrue(imageEquality);
                }

                dicomInputStream.close();

            } catch (IOException ignored) {
                fail();
            } catch (OutOfMemoryError ignored) {
                System.out.println("File too big to be tested! Will skip");
            }
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/archy/ic-encoders-eval/images/dataset_dicom/"})
    void encodeIteratorDicomObjectWithAllTs(String datasetDir) {

        Iterator<File> dicomIterator = getDicomDatasetFiles(datasetDir);

        while (dicomIterator.hasNext()) {
            File dicomFile = dicomIterator.next();
            DicomInputStream dicomInputStream = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));
            DicomInputStream dicomInputStream2 = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));

            DicomObject dicomObjectMeta = assertDoesNotThrow(() -> dicomInputStream.readDicomObject());
            dicomObjectMeta.remove(Tag.TransferSyntaxUID);
            dicomObjectMeta.remove(Tag.PixelData);

            DicomObject originalDicomObject = assertDoesNotThrow(() -> dicomInputStream2.readDicomObject());

            Iterator<DicomObject> encodedDicomObjects = DicomUtils.encodeIteratorDicomObjectWithAllTs(originalDicomObject);

            while (encodedDicomObjects.hasNext()) {
                DicomObject encodedDicomObject = encodedDicomObjects.next();
                assertNotNull(encodedDicomObject);

                encodedDicomObject.remove(Tag.PixelData);
                DicomElement lossyStatusElement = encodedDicomObject.remove(Tag.LossyImageCompression);
                DicomElement lossyMethodElement = encodedDicomObject.remove(Tag.LossyImageCompressionMethod);
                DicomElement lossyRatioElement = encodedDicomObject.remove(Tag.LossyImageCompressionRatio);
                DicomElement transferSyntaxElement = encodedDicomObject.remove(Tag.TransferSyntaxUID);
                String transferSyntaxUid = transferSyntaxElement.getString(dicomObjectMeta.getSpecificCharacterSet(), false);
                NewFormat currentFormat = Arrays.stream(NewFormat.values())
                        .filter(newFormat -> newFormat.getTransferSyntax().uid().equals(transferSyntaxUid))
                        .findFirst()
                        .orElseGet(Assertions::fail);

                assertEquals(lossyStatusElement.getString(encodedDicomObject.getSpecificCharacterSet(), false),
                        "01");
                assertEquals(currentFormat.getMethod(),
                        lossyMethodElement.getString(dicomObjectMeta.getSpecificCharacterSet(), false));
                assertTrue(lossyRatioElement.getFloat(false) >= 1);

                assertEquals(encodedDicomObject, dicomObjectMeta);

            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/archy/ic-encoders-eval/images/dataset_dicom/"})
    @Disabled
    void encodeIteratorDicomInputStreamWithAllTs(String datasetDir) {

        Iterator<File> dicomSingleFrameIterator = getDicomDatasetFiles(datasetDir, 2, true, true),
                dicomMultiFrameIterator = getDicomDatasetFiles(datasetDir, 2, false, true);

        Iterator<File> dicomIterator = new MergeIterator<>(dicomSingleFrameIterator, dicomMultiFrameIterator);

        while (dicomIterator.hasNext()) {
            File dicomFile = dicomIterator.next();
            DicomInputStream dicomInputStream = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));

            DicomObject dicomObjectMeta = assertDoesNotThrow(() -> dicomInputStream.readDicomObject());
            dicomObjectMeta.remove(Tag.TransferSyntaxUID);
            dicomObjectMeta.remove(Tag.PixelData);

            Iterator<DicomInputStream> encodedDicomObjects = DicomUtils.encodeIteratorDicomInputStreamWithAllTs(dicomFile);

            while (encodedDicomObjects.hasNext()) {
                DicomInputStream encodedDicomInputStream = assertDoesNotThrow(encodedDicomObjects::next);
                DicomObject encodedDicomObject = assertDoesNotThrow(() -> encodedDicomInputStream.readDicomObject());
                assertNotNull(encodedDicomObject);

                encodedDicomObject.remove(Tag.PixelData);
                DicomElement lossyStatusElement = encodedDicomObject.remove(Tag.LossyImageCompression);
                DicomElement lossyMethodElement = encodedDicomObject.remove(Tag.LossyImageCompressionMethod);
                DicomElement lossyRatioElement = encodedDicomObject.remove(Tag.LossyImageCompressionRatio);
                DicomElement transferSyntaxElement = encodedDicomObject.remove(Tag.TransferSyntaxUID);
                String transferSyntaxUid = transferSyntaxElement.getString(dicomObjectMeta.getSpecificCharacterSet(), false);
                NewFormat currentFormat = Arrays.stream(NewFormat.values())
                        .filter(newFormat -> newFormat.getTransferSyntax().uid().equals(transferSyntaxUid))
                        .findFirst()
                        .orElseGet(Assertions::fail);

                assertEquals(lossyStatusElement.getString(encodedDicomObject.getSpecificCharacterSet(), false),
                        "01");
                assertEquals(currentFormat.getMethod(),
                        lossyMethodElement.getString(dicomObjectMeta.getSpecificCharacterSet(), false));
                assertTrue(lossyRatioElement.getFloat(false) >= 1);

                assertEquals(encodedDicomObject, dicomObjectMeta);

            }
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {DICOM_DATASET_DIR})
    void writeDicomObjectToTmpFile(String datasetDir) {

        Iterator<File> dicomIterator = getDicomDatasetFiles(datasetDir);

        while (dicomIterator.hasNext()) {
            File dicomFile = dicomIterator.next();
            DicomInputStream dicomInputStream = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));
            DicomObject dicomObject = assertDoesNotThrow(() -> dicomInputStream.readDicomObject());

            File writtenDicomFile = assertDoesNotThrow(() -> DicomUtils.writeDicomObjectToTmpFile(dicomObject));

            assertTrue(assertDoesNotThrow(() -> FileUtils.contentEquals(dicomFile, writtenDicomFile)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {DICOM_DATASET_DIR})
    void testGetDicomDatasetFiles(String datasetDir) {

        byte iterationsLimit = 3;

        Iterator<File> singleFrameDicomIterator = getDicomDatasetFiles(datasetDir, iterationsLimit, true, true),
                multiFrameDicomIterator = getDicomDatasetFiles(datasetDir, iterationsLimit, false, true);

        while (singleFrameDicomIterator.hasNext() && multiFrameDicomIterator.hasNext()) {
            File singleFrameDicomFile = singleFrameDicomIterator.next(),
                    multiFrameDicomFile = multiFrameDicomIterator.next();

            assertTrue(assertDoesNotThrow(() -> DicomUtils.isMultiFrame(multiFrameDicomFile)));
            assertFalse(assertDoesNotThrow(() -> DicomUtils.isMultiFrame(singleFrameDicomFile)));

            DicomInputStream dicomInputStream1 = assertDoesNotThrow(() -> new DicomInputStream(singleFrameDicomFile));
            DicomInputStream dicomInputStream2 = assertDoesNotThrow(() -> new DicomInputStream(multiFrameDicomFile));

            StopTagInputHandler stopAtPixelData = new StopTagInputHandler(Tag.PixelData);
            dicomInputStream1.setHandler(stopAtPixelData);
            dicomInputStream2.setHandler(stopAtPixelData);

            DicomObject dicomObject1 = assertDoesNotThrow(() -> dicomInputStream1.readDicomObject());
            DicomObject dicomObject2 = assertDoesNotThrow(() -> dicomInputStream2.readDicomObject());

            if (dicomObject1.contains(Tag.LossyImageCompression))
                assertEquals(dicomObject1.getString(Tag.LossyImageCompression), "00");
            if (dicomObject2.contains(Tag.LossyImageCompression))
                assertEquals(dicomObject2.getString(Tag.LossyImageCompression), "00");
        }

    }
}