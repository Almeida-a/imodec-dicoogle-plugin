package pt.ua.imodec.util.formats;

import org.dcm4che2.data.TransferSyntax;

public enum NewFormat implements Format {

    JPEG_XL(NewFormatsTS.JPEG_XL_TS, "ISO_18181", "jxl", 1.0f, 7),
    WEBP(NewFormatsTS.WEBP_TS, "webp", "webp", 75, 4),
    AVIF(NewFormatsTS.AVIF_TS, "avif", "avif", 80, 4);

    private final TransferSyntax transferSyntax;
    private final String method, fileExtension, id;
    private final String method, fileExtension;
    private final Number defaultQualityParamValue, defaultSpeedParamValue;

    public Number getDefaultQualityParamValue() {
        return defaultQualityParamValue;
    }

    public Number getDefaultSpeedParamValue() {
        return defaultSpeedParamValue;
    }

    NewFormat(TransferSyntax transferSyntax, String method, String fileExtension, Number defaultQualityParamValue, Number defaultSpeedParamValue) {
        this.transferSyntax = transferSyntax;
        this.method = method;
        this.fileExtension = fileExtension;
        this.defaultQualityParamValue = defaultQualityParamValue;
        this.defaultSpeedParamValue = defaultSpeedParamValue;
        this.id = fileExtension;
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
