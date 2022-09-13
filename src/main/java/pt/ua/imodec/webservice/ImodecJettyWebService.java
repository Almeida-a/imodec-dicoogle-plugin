package pt.ua.imodec.webservice;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.io.DicomInputStream;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.imodec.storage.ImodecStoragePlugin;
import pt.ua.imodec.util.DicomUtils;
import pt.ua.imodec.util.GifSequenceWriter;
import pt.ua.imodec.util.ImageUtils;
import pt.ua.imodec.util.NewFormatsCodecs;
import pt.ua.imodec.util.formats.Format;
import pt.ua.imodec.util.formats.Native;
import pt.ua.imodec.util.formats.NewFormat;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
    private static final String formatIdParameterName = "codec";
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

        ServletContext servletContext = request.getServletContext();
        logger.info("Servlet Real Root Path: '{}'", servletContext.getRealPath("/"));

        DicomInputStream dicomInputStream = extractRequestedDicomFromStorage(request);
        DicomObject dicomObject = DicomUtils.readNonPixelData(extractRequestedDicomFromStorage(request));

        boolean isMultiFrame = dicomObject.getInt(Tag.NumberOfFrames) > 1;

        if (isMultiFrame) {
            response.setContentType("text/html;charset=utf-8");

            String tsUID = dicomObject.getString(Tag.TransferSyntaxUID);

            Iterator<BufferedImage> frameIterator = ImageUtils.loadDicomImageIterator(dicomInputStream);

            File gif = saveToGif(frameIterator, dicomObject.getString(Tag.SOPInstanceUID) + "-" + tsUID);

            PrintWriter printWriter = response.getWriter();

            Optional<Resource> gifResource = Optional.ofNullable(
                    Resource.newResource(servletContext.getResource("/" + gif.getName()))
            );

            printWriter.println("<!DOCTYPE html>");
            printWriter.println("<head/>");
            printWriter.println("<body>");
            if (gifResource.isPresent() && gifResource.get().exists()) {
                printWriter.printf("<img src=\"%s\" alt=\"Image failed!\"/>\n", gif.getName());  // FIXME: 16/09/22 Image always fails
                printWriter.printf("<a href=\"%s\" >Copy and paste this link to view the image locally" +
                        "</a>\n", gifResource.get().getURL());
            }
            else {
                logger.error("Gif file was not found!");
                printWriter.println("Error code 500! Multi-frame image does not exist!");
            }
            printWriter.println("</body>");

            return;
        }

        response.setContentType("image/png");

        InputStream inputStream = extractImageInputStream(dicomInputStream, dicomObject);

        BufferedOutputStream servletOutputStream = new BufferedOutputStream(response.getOutputStream());

        int ch;
        while ((ch = inputStream.read()) != -1)
            servletOutputStream.write(ch);

        servletOutputStream.close();
        inputStream.close();

    }

    /**
     *
     * @param dicomInputStream
     * @param dicomObject is not expected to contain data, only the meta information
     * @return
     * @throws IOException
     */
    private static InputStream extractImageInputStream(DicomInputStream dicomInputStream, DicomObject dicomObject) throws IOException {


        List<NewFormat> newFormatList = Arrays.asList(NewFormat.values());

        List<String> newFormatListTsUids = newFormatList
                .stream()
                .map(NewFormat::getTransferSyntax)
                .map(TransferSyntax::uid)
                .collect(Collectors.toList()
                );

        BufferedImage dicomImage;
        String tsUID = dicomObject.getString(Tag.TransferSyntaxUID);
        boolean isMultiframe = dicomObject.getInt(Tag.NumberOfFrames) > 1;

        if (newFormatListTsUids.contains(tsUID)) {// Case recent formats
            // Parse format uid into format id
            NewFormat chosenFormat = newFormatList
                    .stream()
                    .filter(newFormat -> newFormat.getTransferSyntax().uid().equals(tsUID))
                    .findFirst()
                    .orElseThrow(UnsupportedEncodingException::new);

            if (!isMultiframe)
                dicomImage = NewFormatsCodecs.decodeByteStream(
                        dicomObject.getBytes(Tag.PixelData), chosenFormat
                        );
            else
                dicomImage = NewFormatsCodecs.decodeByteStream(
                        dicomObject.get(Tag.PixelData).getFragment(0), chosenFormat
                );
        } else if (!isMultiframe) {
            dicomImage = ImageUtils.loadDicomImage(dicomInputStream, 0);
        } else {

            Iterator<BufferedImage> frameIterator = ImageUtils.loadDicomImageIterator(dicomInputStream);

            File gif = saveToGif(frameIterator, dicomObject.getString(Tag.SOPInstanceUID)
                    + "-" + tsUID);

            return Files.newInputStream(gif.toPath());

        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(dicomImage, "png", baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     *
     * @param frameIterator iterator with the images
     * @param gifFileBaseName Name of the resulting gif file (w/o the .gif)
     * @throws IOException
     */
    private static File saveToGif(Iterator<BufferedImage> frameIterator, String gifFileBaseName) throws IOException {

        String resourcesUriPath = ImodecJettyPlugin.RESOURCES_URI.getPath();
        File prefixDir = new File(resourcesUriPath);
        if (!prefixDir.exists() && !prefixDir.mkdirs())
                throw new NoSuchFileException("Could not create directory: " + prefixDir);
        prefixDir.deleteOnExit();

        File gif = new File(prefixDir + "/" + gifFileBaseName + ".gif");
        if (gif.exists())
            return gif;

        if (!gif.exists() && !gif.createNewFile())
            throw new AssertionError("Unexpected error!");
        gif.deleteOnExit();

        if (!frameIterator.hasNext())
            throw new NullPointerException("No frames to iterate over!");
        BufferedImage firstFrame = frameIterator.next();

        ImageOutputStream fileOutputStream = new FileImageOutputStream(gif);

        GifSequenceWriter writer = new GifSequenceWriter(fileOutputStream,
                firstFrame.getType(), 50, true);

        writer.writeToSequence(firstFrame);
        while (frameIterator.hasNext())
            writer.writeToSequence(frameIterator.next());

        writer.close();
        fileOutputStream.close();

        return gif;
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
                transferSyntaxUID = request.getParameter(transferSyntaxUIDParameterName),
                codecId = request.getParameter(formatIdParameterName);

        if (transferSyntaxUID == null && codecId != null) {
            Format format = Arrays.stream((Format[]) (NewFormat.values()))
                    .filter(newFormat -> newFormat.getId().equals(codecId))
                    .findFirst()
                    .orElse(Native.UNCHANGED);

            transferSyntaxUID = format.getTransferSyntax().uid();
        } else if (transferSyntaxUID == null)
            transferSyntaxUID = findNativeVersionTS(sopInstanceUID);

        URI uri = URI.create(storageScheme + "://" + sopInstanceUID + "/" + transferSyntaxUID);

        return getDicomInputStream(uri);
    }

    private String findNativeVersionTS(String sopInstanceUID) {
        String transferSyntaxUID;
        Collection<Native> nativeUIDs = Arrays.stream(Native.values())
                .filter(aNative -> aNative.getTransferSyntax() != null)
                .collect(Collectors.toList());

        String imodecStorageScheme = new ImodecStoragePlugin().getScheme();
        ImodecStoragePlugin imodecStorage = ((ImodecStoragePlugin) this.platform.getStorageForSchema(imodecStorageScheme));

        transferSyntaxUID = nativeUIDs.stream().filter(aNative -> {
                    try {
                        return imodecStorage.containsURI(
                                new URI(String.format("%s://%s/%s",
                                        imodecStorageScheme, sopInstanceUID, aNative.getTransferSyntax().uid())
                                )
                        );
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }).findFirst()
                .orElseThrow(NoSuchElementException::new)
                .getTransferSyntax().uid();
        return transferSyntaxUID;
    }

    private DicomInputStream getDicomInputStream(URI uri) throws IOException {
        Iterable<StorageInputStream> files = this.platform.getStorageForSchema(uri).at(uri);

        Iterator<StorageInputStream> storageInputStreamIterator = files.iterator();

        InputStream inputStream = storageInputStreamIterator.next().getInputStream();

        return new DicomInputStream(inputStream);
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface core) {
        this.platform = core;
    }

}
