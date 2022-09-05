package pt.ua.imodec.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.imodec.util.formats.NewFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

public class NewFormatsCodecs {

    private static final Logger logger = LoggerFactory.getLogger(NewFormatsCodecs.class);
    private static final String losslessFormat = "png";

    public static byte[] encodePNGFile(File pngFile, NewFormat chosenFormat, HashMap<String, Number> options)
            throws IOException {

        // Input validation
        assert pngFile.exists(): String.format("PNG file to encode '%s' does not exist.", pngFile);

        String formatExtension = chosenFormat.getFileExtension();
        String pngFilePath = pngFile.getAbsolutePath();
        String encodedFileName = pngFilePath.replace("png", formatExtension);

        String encodingCommand = getCodecCommand(pngFilePath, encodedFileName, formatExtension,
                true, options);

        execute(encodingCommand);

        File encodedImageFile = new File(encodedFileName);

        encodedImageFile.deleteOnExit();

        return Files.readAllBytes(encodedImageFile.toPath());

    }

    private static void execute(String encodingCommand) throws IOException {
        // Execute the command and wait for it to finish
        Process compression = Runtime.getRuntime().exec(encodingCommand);
        try {
            compression.waitFor();
        } catch (InterruptedException ignored) {}

        if (compression.exitValue() != 0) {
            InputStream processErrorStream = compression.getErrorStream();
            logger.error(String.format("Problem executing process: '%s' failed unexpectedly.\n" +
                    "Error stream: %s", compression, processErrorStream));
            System.exit(1);
        }
    }

    private static String getCodecCommand(String inputPath, String outputPath,
                                          String formatExtension, boolean encoding, HashMap<String, Number> options) {
        assert new File(inputPath).exists(): "Input file does not exist!";
        assert !(new File(outputPath).exists()): "Output file already exists!";

        switch (formatExtension) {
            case "jxl":
                if (encoding) {
                    float distance = (float) options.getOrDefault("distance", NewFormat.JPEG_XL
                            .getDefaultQualityParamValue());
                    int effort = (int) options.getOrDefault("effort", NewFormat.JPEG_XL.getDefaultSpeedParamValue());

                    return String.format("cjxl %s %s --effort=%d --distance=%.3f",
                            inputPath, outputPath, effort, distance);
                }
                return String.format("djxl %s %s", inputPath, outputPath);
            case "avif":
                if (encoding) {
                    int quality = (int) options.getOrDefault("quality", NewFormat.AVIF.getDefaultQualityParamValue()),
                            speed = (int) options.getOrDefault("speed", NewFormat.AVIF.getDefaultSpeedParamValue());
                    return String.format("cavif -o %s --quality %d --speed %d %s", outputPath, quality, speed,
                            inputPath);
                }
                return String.format("avif_decode %s %s", inputPath, outputPath);
            case "webp":
                if (encoding) {
                    int quality = (int) options.getOrDefault("quality", NewFormat.WEBP.getDefaultQualityParamValue()),
                            speed = (int) options.getOrDefault("speed", NewFormat.WEBP.getDefaultSpeedParamValue());
                    return String.format("cwebp -q %d -m %d %s -o %s", quality, speed, inputPath, outputPath);
                }
                return String.format("dwebp %s %s", inputPath, outputPath);
            default:
                throw new IllegalArgumentException("Format is not valid!");
        }

    }

    public static BufferedImage decodeByteStream(byte[] bitstream, NewFormat chosenFormat) throws IOException {
        String encodedFileName = String.format("/tmp/imodec/%s.%s", Arrays.hashCode(bitstream),
                chosenFormat.getFileExtension());
        Files.write(Paths.get(encodedFileName), bitstream);
        return decode(encodedFileName, chosenFormat.getFileExtension());
    }

    private static BufferedImage decode(String inputFilePath, String formatExtension) throws IOException {

        String decodedFileName = inputFilePath.replace(formatExtension, losslessFormat);

        String decodingCommand = getCodecCommand(inputFilePath, decodedFileName, formatExtension, false,
                new HashMap<>());

        Process decompression = Runtime.getRuntime().exec(decodingCommand);

        try {
            decompression.waitFor();
        } catch (InterruptedException ignored) {}

        File decodedImageFile = new File(decodedFileName);

        return ImageIO.read(decodedImageFile);

    }

}
