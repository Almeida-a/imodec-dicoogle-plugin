package pt.ua.imodec;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class ImageUtils {

    /**
     *
     * Load dicom (buffered) image
     * Credits:
     *  <a href="https://github.com/bioinformatics-ua/dicoogle/blob/0a5ab168a2c96dd3637c6fa222cdf04323a473ce/dicoogle
     *  /src/main/java/pt/ua/dicoogle/server/web/utils/ImageLoader.java#L97-L106">Source</a>.
     *
     * @param inputStream Stream with the dicom data
     * @return The buffered image
     * @throws IOException if the image format is not DICOM or another IO issue occurred
     */
    public static BufferedImage loadDicomImage(InputStream inputStream) throws IOException {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");
            ImageReader reader = iter.next();
            ImageReadParam param = reader.getDefaultReadParam();
            reader.setInput(imageInputStream, false);
            return reader.read(0, param);
        }
    }

    public static ImageWriter getImageWriter(String formatName) {

        Iterator<ImageWriter> imageWriterIterator = ImageIO.getImageWritersByFormatName(formatName);

        if (!imageWriterIterator.hasNext())
            return null;

        return imageWriterIterator.next();

    }

    public static ImageReader getImageReader(String formatName) {

        Iterator<ImageReader> imageWriterIterator = ImageIO.getImageReadersByFormatName(formatName);

        if (!imageWriterIterator.hasNext())
            return null;

        return imageWriterIterator.next();

    }
}
