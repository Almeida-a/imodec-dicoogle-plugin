package pt.ua.imodec.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class NewFormatsCodecs {

    private static final String losslessFormat = "png";
    public static byte[] encodePNGFile(File tmpImageFile, NewFormat chosenFormat) throws IOException {

        // Input validation
        assert tmpImageFile.exists(): String.format("PNG file to encode '%s' does not exist.", tmpImageFile);

        String tmpFilePath = tmpImageFile.getAbsolutePath();

        String formatExtension;

        switch (chosenFormat) {
            case JPEG_XL:

                formatExtension = "jxl";

                return encode(tmpFilePath, formatExtension);

            case WEBP:

                formatExtension = "webp";

                return encode(tmpFilePath, formatExtension);
            case AVIF:

                formatExtension = "avif";

                return encode(tmpFilePath, formatExtension);
            default:
                throw new IllegalStateException("Unexpected format!");
        }
    }

    private static byte[] encode(String inputFilePath, String formatExtension) throws IOException {

        String encodedFileName = inputFilePath.replace("png", formatExtension);

        String encodingCommand = getCodecCommand(inputFilePath, encodedFileName, formatExtension, true);

        // Execute the command and wait for it to finish
        Process compression = Runtime.getRuntime().exec(encodingCommand);
        try {
            compression.waitFor();
        } catch (InterruptedException ignored) {}

        File encodedImageFile = new File(encodedFileName);
        if (!encodedImageFile.exists())
            throw new IllegalStateException("Encoded image file should be present!");

        encodedImageFile.deleteOnExit();

        return Files.readAllBytes(encodedImageFile.toPath());
    }

    private static String getCodecCommand(String inputPath, String outputPath,
                                          String formatExtension, boolean encoding) {
        assert new File(inputPath).exists(): "Input file does not exist!";
        assert !(new File(outputPath).exists()): "Output file already exists!";

        char codecId;

        if (encoding)
            codecId = 'c';
        else
            codecId = 'd';

        return String.format("%c%s %s %s", codecId, formatExtension, inputPath, outputPath);
    }

    public static BufferedImage decodeByteStream(byte[] bitstream, NewFormat chosenFormat) throws IOException {
        String encodedFileName = String.format("/tmp/imodec/%s", Arrays.hashCode(bitstream) + chosenFormat.getFileExtension());
        Files.write(Paths.get(encodedFileName), bitstream);
        return decode(encodedFileName, chosenFormat.getFileExtension());
    }

    private static BufferedImage decode(String inputFilePath, String formatExtension) throws IOException {

        String decodedFileName = inputFilePath.replace(formatExtension, losslessFormat);

        String decodingCommand = getCodecCommand(inputFilePath, decodedFileName, formatExtension, false);

        Process decompression = Runtime.getRuntime().exec(decodingCommand);

        try {
            decompression.waitFor();
        } catch (InterruptedException ignored) {}

        File decodedImageFile = new File(decodedFileName);

        return ImageIO.read(decodedImageFile);  // TODO make sure this really works

    }

}
