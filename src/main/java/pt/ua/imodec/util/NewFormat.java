package pt.ua.imodec.util;

import org.dcm4che2.data.TransferSyntax;

public enum NewFormat {

    JPEG_XL(NewFormatsTS.JPEG_XL_TS, "ISO_18181"),
    WEBP(NewFormatsTS.WEBP_TS, "webp"),
    AVIF(NewFormatsTS.AVIF, "avif");

    private final TransferSyntax transferSyntax;
    private final String method;

    NewFormat(TransferSyntax transferSyntax, String method) {
        this.transferSyntax = transferSyntax;
        this.method = method;
    }

    public TransferSyntax getTransferSyntax() {
        return transferSyntax;
    }

    public String getMethod() {
        return method;
    }

    private static class NewFormatsTS {

        private static final String AVIF_TS_UID = "1.2.826.0.1.3680043.2.682.104.3";

        private static final String JPEG_XL_TS_UID = "1.2.826.0.1.3680043.2.682.104.1";

        private static final String WEBP_TS_UID = "1.2.826.0.1.3680043.2.682.104.2";

        public static final TransferSyntax JPEG_XL_TS = new TransferSyntax(JPEG_XL_TS_UID,
                false, false, true, false);

        public static final TransferSyntax WEBP_TS = new TransferSyntax(WEBP_TS_UID,
                false, false, true, false);

        public static final TransferSyntax AVIF = new TransferSyntax(AVIF_TS_UID,
                false, false, true, false);

    }
}
