package pt.ua.imodec.util;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

public class FrameIterator implements Iterator<BufferedImage> {

    private int frame_i = 0;
    private final DicomObject dicomObject;

    public FrameIterator(DicomObject dicomObject) {
        this.dicomObject = dicomObject;
    }

    @Override
    public boolean hasNext() {
        return frame_i + 1 < dicomObject.getInt(Tag.NumberOfFrames);
    }

    @Override
    public BufferedImage next() {
        BufferedImage frame;

        // Retrieve the frame
        try {
            frame = ImageUtils.loadDicomImage(dicomObject, frame_i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        frame_i++;
        return frame;
    }

}
