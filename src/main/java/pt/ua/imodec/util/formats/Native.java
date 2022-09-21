package pt.ua.imodec.util.formats;

import org.dcm4che2.data.TransferSyntax;

public enum Native implements Format {

    IMPLICIT_VR_BIG_ENDIAN(TransferSyntax.ImplicitVRBigEndian, "ibe"),
    EXPLICIT_VR_BIG_ENDIAN(TransferSyntax.ExplicitVRBigEndian, "ebe"),
    IMPLICIT_VR_LITTLE_ENDIAN(TransferSyntax.ImplicitVRLittleEndian, "ile"),
    EXPLICIT_VR_LITTLE_ENDIAN(TransferSyntax.ExplicitVRLittleEndian, "ele"),
    UNCHANGED(null, "keep");

    private final TransferSyntax transferSyntax;
    private final String id;

    Native(TransferSyntax transferSyntax, String id) {
        this.transferSyntax = transferSyntax;
        this.id = id;
    }

    @Override
    public TransferSyntax getTransferSyntax() {
        return transferSyntax;
    }

    public String getId() {
        return id;
    }
}
