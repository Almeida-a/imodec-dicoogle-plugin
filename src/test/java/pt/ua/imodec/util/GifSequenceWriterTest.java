package pt.ua.imodec.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class GifSequenceWriterTest {

    @Test
    @Disabled
    void writeToSequence() throws IOException {
        // Gif Image Directory
        String imageDirPath = String.format("/tmp/imodec/%s/", GifSequenceWriter.class.getSimpleName());
        File gifImageDir = new File(imageDirPath);
        if (!gifImageDir.exists()) {
            assertTrue(gifImageDir.mkdirs());
        }
        assertTrue(gifImageDir.exists());

        // Gif Image file
        File gifImageFile = new File(imageDirPath + "/img.gif");
        if (gifImageFile.exists()) {
            assertTrue(gifImageFile.delete());
        }
        assertTrue(gifImageFile.createNewFile());

        byte numberOfFrames = 20;
        Iterator<BufferedImage> mockImages = getMockImagesIterator(numberOfFrames);
        BufferedImage firstImage = mockImages.next();

        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(gifImageFile);

        GifSequenceWriter sequenceWriter = new GifSequenceWriter(
                imageOutputStream, firstImage.getType(), 20, true);

        ArrayList<BufferedImage> frames = new ArrayList<>(numberOfFrames);

        sequenceWriter.writeToSequence(firstImage);
        frames.add(firstImage);

        mockImages.forEachRemaining(bufferedImage -> {
            assertDoesNotThrow(() -> sequenceWriter.writeToSequence(bufferedImage));
            frames.add(bufferedImage);
        });

        sequenceWriter.close();
        imageOutputStream.close();

        ImageInputStream imageInputStream = ImageIO.createImageInputStream(gifImageFile);

        ImageReader gifReader = ImageUtils.getImageReader("gif");
        gifReader.setInput(imageInputStream);
        for (int i = 0; i < numberOfFrames; i++) {
            BufferedImage tmpStoredFrame = frames.get(i);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(tmpStoredFrame, "png", outputStream);

            BufferedImage gifFrame = gifReader.read(i, gifReader.getDefaultReadParam()),
                    storedFrame = ImageIO.read(new ByteArrayInputStream(outputStream.toByteArray()));
            assertEquals(storedFrame, gifFrame);
            assertTrue(ImageUtilsTest.compare(gifFrame, storedFrame));
        }

        imageInputStream.close();
    }

    static Iterator<BufferedImage> getMockImagesIterator(byte numberOfFrames) {
        return new Iterator<BufferedImage>() {
            public final byte totalNumberOfFrames = numberOfFrames;
            private byte framesCounter = 0;
            @Override
            public boolean hasNext() {
                return framesCounter < totalNumberOfFrames;
            }

            @Override
            public BufferedImage next() {
                framesCounter++;
                return ImageUtilsTest.createRandomImage();
            }
        };
    }
}