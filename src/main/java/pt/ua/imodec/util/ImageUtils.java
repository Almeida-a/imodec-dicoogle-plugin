package pt.ua.imodec.util;

import org.apache.commons.io.FileUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.imodec.ImodecPluginSet;
import pt.ua.imodec.datastructs.formats.Format;
import pt.ua.imodec.datastructs.formats.Native;
import pt.ua.imodec.datastructs.formats.NewFormat;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class ImageUtils {

    public static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    /**
     *
     * @param dicomObject Dicom data source
     * @param frame ID of the frame in the dicom object
     * @return A single frame from the dicom object's pixel data
     * @throws IOException If an IO error occurs
     */
    public static BufferedImage loadDicomImage(DicomObject dicomObject, int frame) throws IOException {

        File tmpDicomFile = DicomUtils.saveDicomFile(dicomObject, true);

        DicomInputStream dicomInputStream = new DicomInputStream(tmpDicomFile);

        return DicomUtils.loadDicomImage(dicomInputStream, frame);
    }

    /**
     * Loads the frames with an iterator
     *
     * @param dicomInputStream Dicom file data source
     * @return An iterator containing a frame in each iteration
     * @throws IOException If an IO error occurs
     */
    public static Iterator<BufferedImage> loadDicomImageIterator(DicomInputStream dicomInputStream) throws IOException {

        File file = new File(ImodecPluginSet.TMP_DIR_PATH + "/loadIteratorTmp.dcm");
        file.deleteOnExit();
        FileUtils.copyInputStreamToFile(dicomInputStream, file);

        DicomObject meta = DicomUtils.readNonPixelData(new DicomInputStream(file));

        return new Iterator<BufferedImage>() {

            private int i = 0;
            private final int frames = meta.getInt(Tag.NumberOfFrames);
            private final String transferSyntax = meta.getString(Tag.TransferSyntaxUID);
            private final Format format = Arrays.stream((Format[]) NewFormat.values())
                    .filter(format -> format.getTransferSyntax().uid().equals(transferSyntax))
                    .findFirst()
                    .orElse(Native.UNCHANGED);

            @Override
            public boolean hasNext() {
                return i + 1 < frames;
            }

            @Override
            public BufferedImage next() {
                try {
                    if (format instanceof NewFormat) {
                        try (DicomInputStream inputStream = new DicomInputStream(file)) {
                            return DicomUtils.loadDicomEncodedFrame(inputStream, i, (NewFormat) format);
                        }
                    }
                    return DicomUtils.loadDicomImage(new DicomInputStream(file), i++);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static ImageReader getImageReader(String formatName) {

        Iterator<ImageReader> imageWriterIterator = ImageIO.getImageReadersByFormatName(formatName);

        if (!imageWriterIterator.hasNext())
            throw new NullPointerException(String.format("Format '%s' is not supported by ImageIO!", formatName));

        return imageWriterIterator.next();

    }

}
