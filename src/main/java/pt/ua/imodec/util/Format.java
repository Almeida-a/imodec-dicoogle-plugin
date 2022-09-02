package pt.ua.imodec.util;

import org.dcm4che2.data.TransferSyntax;

public interface Format {

    TransferSyntax getTransferSyntax();

    String getId();
}
