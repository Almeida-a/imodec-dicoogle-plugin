package pt.ua.imodec.util.codec;

import org.apache.commons.lang.NotImplementedException;

public class AvifBuilder implements CommandBuilder {

    private float quality;
    private int speed;
    private String inputFile, outputFile;

    public float getQuality() {
        return quality;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public AvifBuilder(float quality, int speed, String inputFile, String outputFile) {
        this.quality = quality;
        this.speed = speed;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    @Override
    public String getEncodingCommand() {
        return String.format("cavif -o %s --quality %s --speed %s %s", inputFile, quality, speed, outputFile);
    }

    @Override
    public String getDecodingCommand() {
        return String.format("avif_decode -f %s %s", inputFile, outputFile);
    }

    @Override
    public void validate(boolean isForEncoding) {

    }
}
