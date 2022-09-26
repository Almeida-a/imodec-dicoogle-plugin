package pt.ua.imodec.datastructs;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import pt.ua.imodec.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

public class FrameIterator implements Iterator<BufferedImage> {

    private int nextFrame_i = 0;
    private final DicomObject dicomObject;

    public FrameIterator(DicomObject dicomObject) {
        this.dicomObject = dicomObject;
    }

    @Override
    public boolean hasNext() {
        return nextFrame_i < dicomObject.getInt(Tag.NumberOfFrames);
    }

    @Override
    public BufferedImage next() {
        BufferedImage frame;

        // Retrieve the frame
        try {
            frame = ImageUtils.loadDicomImage(dicomObject, nextFrame_i++);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return frame;
    }

}
