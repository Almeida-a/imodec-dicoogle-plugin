package pt.ua.imodec.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class NewFormatsCodecs {
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

    private static byte[] encode(String tmpFilePath, String formatExtension) throws IOException {

        String encodedFileName = tmpFilePath.replace("png", formatExtension);

        String compressCommand = getEncodingCommand(tmpFilePath, encodedFileName, formatExtension);

        Runtime.getRuntime().exec(compressCommand);

        File encodedImage = new File(encodedFileName);

        return Files.readAllBytes(encodedImage.toPath());
    }

    private static String getEncodingCommand(String inputPath, String encodedFileName, String formatExtension) {
        assert new File(inputPath).exists(): "Input path for command does not exist!";
        assert !(new File(encodedFileName).exists()): "Encoded file name already exists!";

        return String.format("c%s %s %s", formatExtension, inputPath, encodedFileName);
    }
}
