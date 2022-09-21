package pt.ua.imodec.util.codec;

public interface CommandBuilder {// TODO: 21/09/22 Input and output file or original / encoded / decoded?

    String getEncodingCommand();

    String getDecodingCommand();

    void validate(boolean isForEncoding);

}
