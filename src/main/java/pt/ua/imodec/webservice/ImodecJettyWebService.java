package pt.ua.imodec.webservice;

import org.dcm4che2.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.imodec.ImageUtils;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.Iterator;

/**
 * Jetty Servlet-based web service, based on bioinformatics-ua/dicoogle-plugin-sample
 *
 * @author André Almeida - almeida.a@ua.pt
 * */

public class ImodecJettyWebService extends HttpServlet implements PlatformCommunicatorInterface {
    private static final Logger logger = LoggerFactory.getLogger(ImodecJettyWebService.class);

    private DicooglePlatformInterface platform;

    public ImodecJettyWebService() {}

    /**
     * Get method
     * <p>
     * Test url: <a href="http://localhost:8080/imodec/view?uid=2.25.145188524015768360381330609360614134902">link</a>.
     *
     * @param request Get request parameters
     * @param response Response data
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        DicomInputStream dicomInputStream = extractRequestedDicom(request);

        if (dicomInputStream == null) return;

        response.setContentType("image/png");

        BufferedImage dicomImage = ImageUtils.loadDicomImage(dicomInputStream);

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

    private DicomInputStream extractRequestedDicom(HttpServletRequest request) throws IOException {
        String SOPInstanceUID = request.getParameter("uid");

        QueryInterface dimProvider = this.platform.getQueryProviderByName("lucene", true);
        Iterator<SearchResult> results = dimProvider.query("SOPInstanceUID:" + SOPInstanceUID).iterator();

        if (!results.hasNext()) {
            // no such file
            logger.error("No dicom file found!");
            return null;
        }

        SearchResult res = results.next();
        URI uri = res.getURI();

        if (uri == null)
            return null;

        return new DicomInputStream(new File(uri));
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface core) {
        this.platform = core;
    }

}
