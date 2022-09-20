package pt.ua.imodec.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.imodec.ImodecPluginSet;
import pt.ua.imodec.util.formats.NewFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
            logger.error("Problem executing process: '{}' failed unexpectedly with error '{}'.\n" +
                    "Error code: %s", encodingCommand, compression.exitValue());
            throw new AssertionError("Unexpected error");
        }
    }

    private static String getCodecCommand(String inputPath, String outputPath,
                                          String formatExtension, boolean encoding, HashMap<String, Number> options) {
        assert new File(inputPath).exists(): "Input file does not exist!";
        assert !(new File(outputPath).exists()): "Output file already exists!";

        switch (formatExtension) {
            case "jxl":
                if (encoding) {
                    Number distance = options.getOrDefault("distance", NewFormat.JPEG_XL
                            .getQualityParamValue());
                    Number effort = options.getOrDefault("effort", NewFormat.JPEG_XL.getSpeedParamValue());

                    return String.format("cjxl %s %s --effort=%s --distance=%s",
                            inputPath, outputPath, effort, distance);
                }
                return String.format("djxl %s %s", inputPath, outputPath);
            case "avif":
                if (encoding) {
                    Number quality = options.getOrDefault("quality", NewFormat.AVIF.getQualityParamValue()),
                            speed = options.getOrDefault("speed", NewFormat.AVIF.getSpeedParamValue());
                    return String.format("cavif -o %s --quality %s --speed %s %s", outputPath, quality, speed,
                            inputPath);
                }
                return String.format("avif_decode %s %s", inputPath, outputPath);
            case "webp":
                if (encoding) {
                    Number quality = options.getOrDefault("quality", NewFormat.WEBP.getQualityParamValue()),
                            speed = options.getOrDefault("speed", NewFormat.WEBP.getSpeedParamValue());
                    return String.format("cwebp -q %s -m %s %s -o %s", quality, speed, inputPath, outputPath);
                }
                return String.format("dwebp %s -o %s", inputPath, outputPath);
            default:
                throw new IllegalArgumentException("Format is not valid!");
        }

    }

    public static BufferedImage decodeByteStream(byte[] bitstream, NewFormat chosenFormat) throws IOException {
        String encodedFileName = String.format("%s/%s.%s", ImodecPluginSet.TMP_DIR_PATH,
                Arrays.hashCode(bitstream), chosenFormat.getFileExtension());
        Files.write(Paths.get(encodedFileName), bitstream);
        return decode(encodedFileName, chosenFormat.getFileExtension());
    }

    private static BufferedImage decode(String inputFilePath, String formatExtension) throws IOException {

        String decodedFileName = inputFilePath + "." + losslessFormat;
        File decodedImageFile = new File(decodedFileName);
        if (decodedImageFile.exists())
            return ImageIO.read(decodedImageFile);

        String decodingCommand = getCodecCommand(inputFilePath, decodedFileName, formatExtension, false,
                new HashMap<>());

        execute(decodingCommand);

        decodedImageFile.deleteOnExit();

        return ImageIO.read(decodedImageFile);

    }

}
