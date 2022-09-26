package pt.ua.imodec.util;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import pt.ua.imodec.ImodecPluginSet;
import pt.ua.imodec.datastructs.formats.NewFormat;
import pt.ua.imodec.webservice.ImodecJettyPlugin;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MiscUtils {

    private static final Logger logger = LoggerFactory.getLogger(MiscUtils.class);

    /**
     *
     * Same as file.createNewFile, however creates parents directories if they don't exist
     *
     * @param file File to be created
     * @return Status flag
     * @throws IOException from file.createNewFile
     */
    public static boolean createNewFile(File file, boolean overwrite) throws IOException {
        File parentFile = file.getParentFile();

        if (!parentFile.exists())
            if (!parentFile.mkdirs())
                throw new IOException(String.format("Unexpected error: Could not mkdir of dir '%s'.", parentFile));

        if (overwrite && file.exists() && !file.delete())
                logger.error("Could not delete temporary file with the same name that was trying to create!");

        return file.createNewFile();
    }

    /**
     * Sleep while the provided condition is true.
     *  The program re-checks if the condition is true after 100ms
     * @param booleanSupplier Expression to be periodically evaluated.
     *                        Note: This is a supplier instead of a boolean because a boolean is just
     *                        a static variable, and thus, would have a static value. On the other hand, a boolean
     *                        supplier is associated to an executable expression.
     */
    public static void sleepWhile(Supplier<Boolean> booleanSupplier) {
        while (booleanSupplier.get()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     *
     * @param formatId The format in its file extension form
     * @return The default options of the encoder specified in "encoding-options.yaml"
     */
    public static Map<String, Number> getOptions(String formatId) {
        Map<String, Map<String, Number>> options = getOptions();

        return options.get(formatId);
    }

    /**
     *
     * @return A Map object containing the contents of "encoding-options.yaml"
     */
    public static Map<String, Map<String, Number>> getOptions() {
        Yaml yaml = new Yaml();

        InputStream inputStream = MiscUtils.class
                .getClassLoader()
                .getResourceAsStream("encoding-options.yaml");

        return yaml.load(inputStream);
    }

    public static Number gracefulCast(Number number, Class<? extends Number> toType) {
        // TODO: 21/09/22 Find a way to refactor this into a switch statement

        if (Float.class.equals(toType))
            return number.floatValue();
        else if (Double.class.equals(toType))
            return number.doubleValue();
        else if (Integer.class.equals(toType))
            return number.intValue();
        else if (Short.class.equals(toType))
            return number.shortValue();
        else if (Byte.class.equals(toType))
            return number.byteValue();
        logger.error("Invalid type to cast to!");
        return number;
    }

    /**
     *
     * @param bos Large output stream
     * @return Input stream
     */
    public static InputStream getInputStreamFromLarge(ByteArrayOutputStream bos) throws IOException {
        File file = new File(
                String.format("%s/blobs_%s.tmp", ImodecPluginSet.TMP_DIR_PATH, Instant.now()));

        if (!file.getParentFile().exists() && !file.getParentFile().mkdir())
            throw new IOException("Could not create dir: " + ImodecPluginSet.TMP_DIR_PATH);

        if (!file.createNewFile())
            throw new FileAlreadyExistsException("File -> "+file.getName());

        FileOutputStream fileOutputStream = new FileOutputStream(file);
        bos.writeTo(fileOutputStream);
        InputStream inputStream = Files.newInputStream(file.toPath());

        file.deleteOnExit();

        return inputStream;
    }

    /**
     *
     * @param dicomInputStream
     * @param dicomObject is not expected to contain data, only the meta information
     * @return
     * @throws IOException
     */
    public static InputStream extractImageInputStream(DicomInputStream dicomInputStream, DicomObject dicomObject) throws IOException {


        List<NewFormat> newFormatList = Arrays.asList(NewFormat.values());

        List<String> newFormatListTsUids = newFormatList
                .stream()
                .map(NewFormat::getTransferSyntax)
                .map(TransferSyntax::uid)
                .collect(Collectors.toList()
                );

        BufferedImage dicomImage;
        String tsUID = dicomObject.getString(Tag.TransferSyntaxUID);
        boolean isMultiframe = dicomObject.getInt(Tag.NumberOfFrames) > 1;

        if (newFormatListTsUids.contains(tsUID)) {// Case recent formats
            // Parse format uid into format id
            NewFormat chosenFormat = newFormatList
                    .stream()
                    .filter(newFormat -> newFormat.getTransferSyntax().uid().equals(tsUID))
                    .findFirst()
                    .orElseThrow(UnsupportedEncodingException::new);

            if (!isMultiframe)
                dicomImage = NewFormatsCodecs.decodeByteStream(
                        dicomObject.getBytes(Tag.PixelData), chosenFormat
                        );
            else
                dicomImage = NewFormatsCodecs.decodeByteStream(
                        dicomObject.get(Tag.PixelData).getFragment(0), chosenFormat
                );
        } else if (!isMultiframe) {
            dicomImage = DicomUtils.loadDicomImage(dicomInputStream, 0);
        } else {

            Iterator<BufferedImage> frameIterator = ImageUtils.loadDicomImageIterator(dicomInputStream);

            File gif = saveToGif(frameIterator, dicomObject.getString(Tag.SOPInstanceUID)
                    + "-" + tsUID);

            return Files.newInputStream(gif.toPath());

        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(dicomImage, "png", baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     *
     * @param frameIterator iterator with the images
     * @param gifFileBaseName Name of the resulting gif file (w/o the .gif)
     * @throws IOException If an IO error occurs
     */
    public static File saveToGif(Iterator<BufferedImage> frameIterator, String gifFileBaseName) throws IOException {

        String resourcesUriPath = ImodecJettyPlugin.RESOURCES_URI.getPath();
        File prefixDir = new File(resourcesUriPath);
        if (!prefixDir.exists() && !prefixDir.mkdirs())
                throw new NoSuchFileException("Could not create directory: " + prefixDir);
        prefixDir.deleteOnExit();

        File gif = new File(prefixDir + "/" + gifFileBaseName + ".gif");
        if (gif.exists())
            return gif;

        if (!gif.exists() && !gif.createNewFile())
            throw new AssertionError("Unexpected error!");
        gif.deleteOnExit();

        if (!frameIterator.hasNext())
            throw new NullPointerException("No frames to iterate over!");
        BufferedImage firstFrame = frameIterator.next();

        ImageOutputStream fileOutputStream = new FileImageOutputStream(gif);

        GifSequenceWriter writer = new GifSequenceWriter(fileOutputStream,
                firstFrame.getType(), 50, true);

        writer.writeToSequence(firstFrame);
        while (frameIterator.hasNext())
            writer.writeToSequence(frameIterator.next());

        writer.close();
        fileOutputStream.close();

        return gif;
    }

    /**
     * Clones the provided input stream
     *
     * @param inputStream original IS
     * @return cloned IS (different object, same content)
     * @throws IOException If an IO error occurs
     */
    public static InputStream cloneInputStream(InputStream inputStream) throws IOException {

        int inputStreamSize = inputStream.available();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(inputStreamSize);

        byte[] data = new byte[inputStreamSize];

        int ignored = inputStream.read(data);

        byteArrayOutputStream.write(data);

        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}
