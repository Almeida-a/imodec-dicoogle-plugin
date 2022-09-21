package pt.ua.imodec.util.codec;

import org.apache.commons.lang.NotImplementedException;

public class JpegXLBuilder implements CommandBuilder {

    private float distance;
    private byte effort;
    private String inputFile, outputFile;

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public int getEffort() {
        return effort;
    }

    public void setEffort(int effort) {
        this.effort = (byte) effort;
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

    public JpegXLBuilder(float distance, int effort, String inputFile, String outputFile) {
        this.distance = distance;
        this.effort = (byte) effort;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    @Override
    public String getEncodingCommand() {
        return String.format("cjxl %s %s --effort=%s --distance=%s", inputFile, outputFile, effort, distance);
    }

    @Override
    public String getDecodingCommand() {
        return String.format("djxl %s %s", inputFile, outputFile);
    }

    @Override
    public void validate(boolean isForEncoding) {
        if (isForEncoding) {
            throw new NotImplementedException();
        }
        throw new NotImplementedException();
    }
}
