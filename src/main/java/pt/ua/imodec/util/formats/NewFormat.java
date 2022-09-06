package pt.ua.imodec.util.formats;

import org.dcm4che2.data.TransferSyntax;
import pt.ua.imodec.util.MiscUtils;

public enum NewFormat implements Format {

    JPEG_XL(NewFormatsTS.JPEG_XL_TS, "ISO_18181",
            "jxl", "distance", "effort", Float.class),
    WEBP(NewFormatsTS.WEBP_TS, "webp",
            "webp", "quality", "speed", Byte.class),
    AVIF(NewFormatsTS.AVIF_TS, "avif",
            "avif", "quality", "speed", Byte.class);

    private final TransferSyntax transferSyntax;
    private final String method, fileExtension, id, qualityParamName, speedParamName;

    private Number qualityParamValue, speedParamValue;
    private final Class<? extends Number> qualityParamType;

    NewFormat(TransferSyntax transferSyntax, String method, String fileExtension, String qualityParamName,
              String speedParamName, Class<? extends Number> qualityValueType) {
        this.transferSyntax = transferSyntax;
        this.method = method;
        this.fileExtension = fileExtension;
        this.id = fileExtension;
        this.qualityParamName = qualityParamName;
        Number number = MiscUtils.getOptions(fileExtension).get(qualityParamName);
        this.qualityParamValue = MiscUtils.gracefulCast(number, qualityValueType);
        this.qualityParamType = qualityValueType;
        this.speedParamName = speedParamName;
        this.speedParamValue = MiscUtils.getOptions(fileExtension).get(speedParamName).byteValue();
    }

    @Override
    public TransferSyntax getTransferSyntax() {
        return transferSyntax;
    }

    public String getMethod() {
        return method;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    @Override
    public String getId() {
        return id;
    }

    public Number getQualityParamValue() {
        return qualityParamValue;
    }

    public Number getSpeedParamValue() {
        return speedParamValue;
    }

    public void setQualityParamValue(Number qualityParamValue) {
        this.qualityParamValue = MiscUtils.gracefulCast(qualityParamValue, qualityParamType);
    }

    public void setSpeedParamValue(Number speedParamValue) {
        this.speedParamValue = speedParamValue.byteValue();
    }

    public String getQualityParamName() {
        return qualityParamName;
    }

    public String getSpeedParamName() {
        return speedParamName;
    }

    private static class NewFormatsTS {

        private static final String AVIF_TS_UID = "1.2.826.0.1.3680043.2.682.104.3";

        private static final String JPEG_XL_TS_UID = "1.2.826.0.1.3680043.2.682.104.1";

        private static final String WEBP_TS_UID = "1.2.826.0.1.3680043.2.682.104.2";

        public static final TransferSyntax JPEG_XL_TS = new TransferSyntax(JPEG_XL_TS_UID,
                false, false, true, false);

        public static final TransferSyntax WEBP_TS = new TransferSyntax(WEBP_TS_UID,
                false, false, true, false);

        public static final TransferSyntax AVIF_TS = new TransferSyntax(AVIF_TS_UID,
                false, false, true, false);

    }
}
