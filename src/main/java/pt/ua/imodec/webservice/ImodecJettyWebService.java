package pt.ua.imodec.webservice;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.imodec.storage.ImodecStoragePlugin;
import pt.ua.imodec.util.ImageUtils;
import pt.ua.imodec.util.formats.NewFormat;
import pt.ua.imodec.util.NewFormatsCodecs;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Jetty Servlet-based web service, based on bioinformatics-ua/dicoogle-plugin-sample
 *
 * @author André Almeida - almeida.a@ua.pt
 * */

public class ImodecJettyWebService extends HttpServlet implements PlatformCommunicatorInterface {
    private static final Logger logger = LoggerFactory.getLogger(ImodecJettyWebService.class);
    private static final String sopInstanceUIDParameterName = "siuid";
    private static final String transferSyntaxUIDParameterName = "tsuid";
    public static final String storageScheme = new ImodecStoragePlugin().getScheme();

    private DicooglePlatformInterface platform;

    public ImodecJettyWebService() {}

    /**
     * Get method
     * <p>
     * Test url: <a href="http://localhost:8080/imodec/view?uid=2.25.145188524015768360381330609360614134902">link</a>.
     * Test url jxl: <a href="http://localhost:8080/imodec/view?uid=2.25.235681777654741115596561171269457636934">link</a>.
     *
     * @param request Get request parameters
     * @param response Response data
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        DicomInputStream dicomInputStream = extractRequestedDicomFromStorage(request);
        DicomObject dicomObject = extractRequestedDicomFromStorage(request).readDicomObject();

        response.setContentType("image/png");

        List<NewFormat> newFormatList = Arrays.asList(NewFormat.values());

        List<String> newFormatListTsUids = newFormatList
                .stream()
                .map(NewFormat::getTransferSyntax)
                .map(TransferSyntax::uid)
                .collect(Collectors.toList()
        );

        BufferedImage dicomImage;
        String tsUID = dicomObject.getString(Tag.TransferSyntaxUID);

        if (newFormatListTsUids.contains(tsUID)) {// Case recent formats
            // Parse format uid into format id
            NewFormat chosenFormat = newFormatList
                    .stream()
                    .filter(newFormat -> newFormat.getTransferSyntax().uid().equals(tsUID))
                    .findFirst().get();

            dicomImage = NewFormatsCodecs.decodeByteStream(
                    dicomObject.getBytes(Tag.PixelData), chosenFormat
                    );
        }
        else
            dicomImage = ImageUtils.loadDicomImage(dicomInputStream);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(dicomImage, "png", baos);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
        BufferedOutputStream servletOutputStream = new BufferedOutputStream(response.getOutputStream());

        int ch;
        while ((ch=inputStream.read()) != -1)
            servletOutputStream.write(ch);

        servletOutputStream.close();
        inputStream.close();

    }

    /**
     * Analyzes the request and returns the associated dicom object
     *
     * @param request
     * @return
     * @throws IOException
     */
    private DicomInputStream extractRequestedDicomFromStorage(HttpServletRequest request) throws IOException {
        String sopInstanceUID = request.getParameter(sopInstanceUIDParameterName),
                transferSyntaxUID = request.getParameter(transferSyntaxUIDParameterName);

        URI uri = URI.create(storageScheme + "://" + sopInstanceUID + "/" + transferSyntaxUID);

        Iterable<StorageInputStream> files = this.platform.getStorageForSchema(uri).at(uri);

        Iterator<StorageInputStream> storageInputStreamIterator = files.iterator();

        InputStream inputStream = storageInputStreamIterator.next().getInputStream();

        return new DicomInputStream(inputStream);
    }

    private DicomInputStream extractRequestedDicomFromIndex(HttpServletRequest request) throws IOException {

        String SOPInstanceUID = request.getParameter("uid");

        QueryInterface dimProvider = this.platform.getQueryProviderByName("lucene", true);
        Iterator<SearchResult> results = dimProvider.query("SOPInstanceUID:" + SOPInstanceUID).iterator();

        if (!results.hasNext())
            throw new NoSuchElementException("No dicom file found!");

        SearchResult res = results.next();
        URI uri = res.getURI();

        if (uri == null)
            throw new NullPointerException("Null uri for the requested dicom object!");

        return new DicomInputStream(new File(uri));
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface core) {
        this.platform = core;
    }

}
