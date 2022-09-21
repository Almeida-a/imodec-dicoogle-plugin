package pt.ua.imodec.util.codec;

import org.apache.commons.lang.NotImplementedException;

public class WebpBuilder implements CommandBuilder {

    private byte method;

    private float quality;
    private String inputFile, outputFile;

    public void setMethod(int method) {
        this.method = (byte) method;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }

    public int getMethod() {
        return method;
    }

    public float getQuality() {
        return quality;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public WebpBuilder(byte method, float quality, String inputFile, String outputFile) {
        this.method = method;
        this.quality = quality;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    @Override
    public String getEncodingCommand() {
        validate(true);

        return String.format("cwebp -q %s -m %s %s -o %s", quality, method, inputFile, outputFile);
    }

    @Override
    public String getDecodingCommand() {
        validate(false);

        return String.format("dwebp %s -o %s", inputFile, outputFile);
    }

    /**
     * If invalid parameters, throwing the proper exceptions for each error
     * @param isForEncoding The perspective for attributes' validation.
     *                     Whether the parameters are for encoding (true) or otherwise for decoding (false)
     */
    public void validate(boolean isForEncoding) {
        if (isForEncoding) {
            throw new NotImplementedException();
        }
        throw new NotImplementedException();
    }
}
