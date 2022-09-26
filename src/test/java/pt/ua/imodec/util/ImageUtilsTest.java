package pt.ua.imodec.util;

import org.dcm4che2.data.ConfigurationError;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilsTest {

    public static boolean compare(BufferedImage image1, BufferedImage image2) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image1, "raw", byteArrayOutputStream);
        byte[] img1Data = byteArrayOutputStream.toByteArray();

        byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image2, "raw", byteArrayOutputStream);
        byte[] img2Data = byteArrayOutputStream.toByteArray();

        return Arrays.equals(img1Data, img2Data);
    }

    @Test
    void getImageReader() {

        for (String formatName : ImageIO.getReaderFormatNames()) {

            ImageReader imageReader = ImageUtils.getImageReader(formatName);

            // Get the same image reader manually
            Iterator<ImageReader> imageWriterIterator = ImageIO.getImageReadersByFormatName(formatName);
            assertTrue(imageWriterIterator.hasNext());
            ImageReader imageReader1 = imageWriterIterator.next();

            assertEquals(imageReader.getClass(), imageReader1.getClass());

        }

    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/archy/ic-encoders-eval/images/dataset_dicom/"})
    void loadDicomImage(String datasetDir) {
        Iterator<File> dicomIterator = DicomUtilsTest.getDicomDatasetFiles(datasetDir);

        while (dicomIterator.hasNext()) {
            File dicomFile = dicomIterator.next();
            DicomInputStream dicomInputStream = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));
            DicomInputStream dicomInputStream2 = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));

            try {
                DicomObject dicomObject = assertDoesNotThrow(() -> dicomInputStream.readDicomObject());

                BufferedImage dicomImageLoadedByTestedFunction = assertDoesNotThrow(
                        () -> ImageUtils.loadDicomImage(dicomObject, 0));

                BufferedImage dicomImageLoadedManually = assertDoesNotThrow(
                        () -> DicomUtils.loadDicomImage(dicomInputStream2, 0));

                boolean compare = assertDoesNotThrow(
                        () -> compare(dicomImageLoadedManually, dicomImageLoadedByTestedFunction));
                assertTrue(compare);

            } catch (OutOfMemoryError error) {
                System.out.println("Dicom file too big! Skipping.");
            }

            assertDoesNotThrow(dicomInputStream::close);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/home/archy/ic-encoders-eval/images/dataset_dicom/"})
    void loadDicomImageIterator(String datasetDir) {

        Iterator<File> dicomIterator = DicomUtilsTest.getDicomDatasetFiles(datasetDir);

        while (dicomIterator.hasNext()) {

            File dicomFile = dicomIterator.next();
            DicomInputStream dicomInputStream = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));
            DicomInputStream dicomInputStream1 = assertDoesNotThrow(() -> new DicomInputStream(dicomFile));


            DicomObject dicomObject;
            try {
                dicomObject = dicomInputStream1.readDicomObject();
                assertDoesNotThrow(dicomInputStream1::close);

                Iterator<BufferedImage> imageIterator = assertDoesNotThrow(
                        () -> ImageUtils.loadDicomImageIterator(dicomInputStream));

                AtomicInteger i = new AtomicInteger();
                while (imageIterator.hasNext()) {
                    if (!DicomUtilsTest.isReadable(dicomObject)) {
                        assertThrows(ConfigurationError.class,
                                () -> ImageUtils.loadDicomImage(dicomObject, i.getAndIncrement()));
                        continue;
                    }
                    BufferedImage frame = assertDoesNotThrow(
                            () -> ImageUtils.loadDicomImage(dicomObject, i.getAndIncrement()));
                    assertEquals(frame, imageIterator.next());
                }
            } catch (OutOfMemoryError e) {
                System.out.println("Dicom object was too big to be retrieved! Skipping...");
            } catch (IOException e) {
                fail("Unexpected IOException!");
            }
            assertDoesNotThrow(dicomInputStream::close);
        }
    }

    /**
     * <a href="https://www.geeksforgeeks.org/image-processing-in-java-creating-a-random-pixel-image/">Credits</a>.
     * @return Random image of type BufferedImage
     */
    static BufferedImage createRandomImage() {
        // Image file dimensions
        int width = 640, height = 320;

        // Create buffered image object
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // create random values pixel by pixel
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                // generating values less than 256
                int r = (int)(Math.random()*256);
                int g = (int)(Math.random()*256);
                int b = (int)(Math.random()*256);

                //pixel
                int p = (r<<16) | (g<<8) | b;

                img.setRGB(x, y, p);
            }
        }
        return img;
    }

}