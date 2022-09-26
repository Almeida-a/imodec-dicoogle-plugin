package pt.ua.imodec.datastructs.formats;

import org.dcm4che2.data.TransferSyntax;

public interface Format {

    TransferSyntax getTransferSyntax();

    String getId();
}
