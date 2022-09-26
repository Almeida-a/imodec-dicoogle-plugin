package pt.ua.imodec.util;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.ua.imodec.datastructs.formats.NewFormat;
import pt.ua.imodec.util.validators.CodecsInstalledValidator;
import pt.ua.imodec.util.validators.OSValidator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewFormatsCodecsTest {

    public static final String TESTS_DIR = "/tmp/imodec/tests/";

    @BeforeAll
    static void setTestsDir() {
        File testsDirFile = new File(TESTS_DIR);

        if (testsDirFile.exists())
            return;

        assertTrue(assertDoesNotThrow(testsDirFile::mkdirs));
        assertTrue(testsDirFile::isDirectory);
        testsDirFile.deleteOnExit();
    }

    @BeforeAll
    static void checkCodecsAvailability() {
        Assumptions.assumeTrue(CodecsInstalledValidator.validate());
        Assumptions.assumeTrue(OSValidator.validate());
    }

    @Test
    void encodeDecode() {

        byte numberOfImages = 4;
        Iterator<BufferedImage> imageIterator = GifSequenceWriterTest.getMockImagesIterator(numberOfImages);

        for (int i = 0; imageIterator.hasNext(); i++) {
            BufferedImage originalImage = imageIterator.next();
            File pngFile = new File(String.format(TESTS_DIR + "testImage%d.png", i));
            pngFile.deleteOnExit();
            assertDoesNotThrow(() -> ImageIO.write(originalImage, "png", pngFile));
            BufferedImage pngLoadedImage = assertDoesNotThrow(() -> ImageIO.read(pngFile));

            HashMap<String, Number> options = new HashMap<>();
            options.put("distance", 0.0);
            byte[] codeStream = assertDoesNotThrow(() -> NewFormatsCodecs.encodePNGFile(pngFile, NewFormat.JPEG_XL, options));

            BufferedImage decodedImage = assertDoesNotThrow(
                    () -> NewFormatsCodecs.decodeByteStream(codeStream, NewFormat.JPEG_XL));

            assertTrue(assertDoesNotThrow(
                    () -> ImageUtilsTest.compare(pngLoadedImage, decodedImage)));
        }

    }
}