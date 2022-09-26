package pt.ua.imodec.util;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class MiscUtilsTest {

    public static final String TMP_IMODEC_TEST_PATH = "/tmp/imodec/test/";

    @BeforeAll
    static void createTestDirectory() {
        File testDirectory = new File(TMP_IMODEC_TEST_PATH);

        assertTrue(testDirectory.mkdirs());
        assertTrue(testDirectory.exists());
        assertTrue(testDirectory.isDirectory());

        testDirectory.deleteOnExit();
    }

    @AfterAll
    static void cleanUp() {
        File testDir = new File(TMP_IMODEC_TEST_PATH);
        assertTrue(testDir.exists());
        assertDoesNotThrow(() -> FileUtils.deleteDirectory(testDir));
    }

    @Test
    void createNewFile() throws IOException {
        File testDir = new File(TMP_IMODEC_TEST_PATH);

        assertTrue(testDir.exists());
        File testTargetFile = new File(TMP_IMODEC_TEST_PATH + "a/b/c.txt");

        assertFalse(testTargetFile.exists());
        assertTrue(MiscUtils.createNewFile(testTargetFile, false));
        assertTrue(testTargetFile.exists());

        assertFalse(MiscUtils.createNewFile(testTargetFile, false));
        assertTrue(testTargetFile.exists());

        assertTrue(MiscUtils.createNewFile(testTargetFile, true));
        assertTrue(testTargetFile.exists());

        assertTrue(testTargetFile.delete());
        assertFalse(testTargetFile.exists());

    }

    @Test
    void sleepWhile() {

        byte defineSeconds = 2;  // Arbitrary value (preferably non-negative)
        Instant aFewSecondsLater = Instant.now().plusSeconds(defineSeconds);
        Supplier<Boolean> booleanSupplier = () -> Instant.now().isBefore(aFewSecondsLater);

        MiscUtils.sleepWhile(booleanSupplier);  // Wait ${defineSeconds} seconds
        assertTrue(Instant.now().isAfter(aFewSecondsLater));

    }

    @Test
    void getOptions() {
        Set<String> formats = MiscUtils.getOptions().keySet();
        assertEquals(formats, new HashSet<>(Arrays.asList("jxl", "avif", "webp")));

        Map<String, Number> optionsJxl = MiscUtils.getOptions("jxl");
        Map<String, Number> optionsAvif = MiscUtils.getOptions("avif");
        Map<String, Number> optionsWebp = MiscUtils.getOptions("webp");

        assertEquals(optionsJxl.keySet(), new HashSet<>(Arrays.asList("distance", "effort")));
        assertEquals(optionsAvif.keySet(), new HashSet<>(Arrays.asList("quality", "speed")));
        assertEquals(optionsWebp.keySet(), new HashSet<>(Arrays.asList("quality", "speed")));
    }

    @Test
    void gracefulCast() {
        Byte aByte = 1;
        Short aShort = 1;
        Integer anInteger = 1;
        Float aFloat = 1.0f;
        Double aDouble = 1.0;

        List<Number> numbers = Arrays.asList(aByte, aShort, anInteger, aFloat, aDouble);

        List<? extends Class<? extends Number>> numberClasses = Arrays.asList(
                Byte.class, Short.class, Integer.class, Float.class, Double.class
        );

        // Exhaustive test: cast every number to each other class
        for (Number number : numbers) {
            numberClasses.stream().filter(clazz -> !clazz.isInstance(number)).forEach(clazz -> {
                Number castedNumber = MiscUtils.gracefulCast(number, clazz);
                assertNotEquals(castedNumber, number);
                assertInstanceOf(clazz, castedNumber);
            });
            // Unsupported type (for now): Long
            Number otherNumber = MiscUtils.gracefulCast(number, Long.class);
            assertEquals(otherNumber, number);
        }

    }

    @Test
    void getInputStreamFromLarge() throws IOException {
        int aMega = (int) Math.pow(2, 20), aKilo = (int) Math.pow(2,10);
        ByteArrayOutputStream largeSizedBaos = new ByteArrayOutputStream(aMega);

        Random random = new Random();

        // Original data
        byte[] originalBytes = new byte[aKilo];
        random.nextBytes(originalBytes);

        ByteArrayOutputStream mediumSizedBaos = new ByteArrayOutputStream(aKilo);
        mediumSizedBaos.write(originalBytes);

        assertDoesNotThrow(() -> MiscUtils.getInputStreamFromLarge(largeSizedBaos));
        InputStream inputStream = MiscUtils.getInputStreamFromLarge(mediumSizedBaos);

        byte[] inputStreamByteArray = new byte[aKilo];
        int bytesRead = inputStream.read(inputStreamByteArray);
        inputStream.close();

        assertEquals(bytesRead, aKilo);
        assertArrayEquals(originalBytes, inputStreamByteArray);
    }

    @ParameterizedTest
    @ValueSource(strings = {TestCommons.DICOM_DATASET_DIR})
    void cloneInputStream(String datasetDir) {
        Iterator<File> filesDataset = DicomUtilsTest.getDicomDatasetFiles(datasetDir);

        while (filesDataset.hasNext()) {
            File file = filesDataset.next();
            // It is presumed that i1 == i2
            InputStream inputStream1 = assertDoesNotThrow(() -> FileUtils.openInputStream(file));
            InputStream inputStream2 = assertDoesNotThrow(() -> FileUtils.openInputStream(file));
            InputStream inputStream3 = assertDoesNotThrow(() -> MiscUtils.cloneInputStream(inputStream1));

            byte[] data2 = new byte[assertDoesNotThrow(inputStream2::available)];
            assertDoesNotThrow(() -> inputStream2.read(data2));
            byte[] data3 = new byte[assertDoesNotThrow(inputStream3::available)];
            assertDoesNotThrow(() -> inputStream3.read(data3));

            assertArrayEquals(data2, data3);

            assertDoesNotThrow(inputStream1::close);
            assertDoesNotThrow(inputStream2::close);
            assertDoesNotThrow(inputStream3::close);
        }
    }
}