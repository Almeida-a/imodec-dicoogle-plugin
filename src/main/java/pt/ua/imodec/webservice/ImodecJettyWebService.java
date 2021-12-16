package pt.ua.imodec.webservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Jetty Servlet-based web service, based on bioinformatics-ua/dicoogle-plugin-sample
 *
 * @author André Almeida - <almeida.a@ua.pt>
 * */

public class ImodecJettyWebService extends HttpServlet implements PlatformCommunicatorInterface {
    private static final Logger logger = LoggerFactory.getLogger(ImodecJettyWebService.class);

    private DicooglePlatformInterface platform;

    public ImodecJettyWebService() {}

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        // ...
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface core) {
        this.platform = core;
    }

}
