package com.nxttxn.axis2.transport.vertx;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;


import java.net.URL;
import java.nio.file.Paths;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */

//To start we removed actual support for the connection factory so you cannot have named connections.
//Ideally we'd go back to this at somepoint as vertx http clients are good candidates for a factory
public class VertxConnectionFactory {
    private final ParameterInclude trpDesc;
    private Logger log = LoggerFactory.getLogger(VertxConnectionFactoryManager.class);
    private String name;
    private final String host;
    private final String port;
    private String keystorePath;
    private String keystorePassword;
    private String truststorePath;
    private String truststorePassword;
    private String path;
    private boolean trustAll;
    private final Vertx vertx;


    public VertxConnectionFactory(ParameterInclude trpDesc) {

        this.trpDesc = trpDesc;
        vertx = (Vertx) trpDesc.getParameter(VertxUtils.VERTX).getValue();
        host = (String) trpDesc.getParameter(VertxConstants.VERTX_HOST_NAME).getValue();
        port = (String) trpDesc.getParameter(VertxConstants.VERTX_PORT).getValue();
        if (trpDesc.getParameter(VertxListener.PATH) != null) {
            path = (String) trpDesc.getParameter(VertxListener.PATH).getValue();
        }
        final Parameter keyStoreParam = trpDesc.getParameter(VertxConstants.VERTX_KEYSTORE_PATH);
        if (keyStoreParam != null) {
            keystorePath = (String) keyStoreParam.getValue();
            keystorePassword = (String) trpDesc.getParameter(VertxConstants.VERTX_KEYSTORE_PASSWORD).getValue();
        }

        final Parameter trustStoreParam = trpDesc.getParameter(VertxConstants.VERTX_TRUSTSTORE_PATH);
        if (trustStoreParam != null) {
            truststorePath = (String) trustStoreParam.getValue();
            truststorePassword = (String) trpDesc.getParameter(VertxConstants.VERTX_TRUSTSTORE_PASSWORD).getValue();
        }

        final Parameter trustAllParam = trpDesc.getParameter(VertxConstants.VERTX_TRUST_ALL);
        if (trustAllParam != null) {
            final String trustAllValue = (String) trustAllParam.getValue();
            trustAll = Boolean.parseBoolean(trustAllValue);
        }
    }

    public String getName() {
        return name;
    }

    public HttpServer getHttpServer(Handler<HttpServerRequest> requestHandler) {
        final RouteMatcher routeMatcher = new RouteMatcher();
        log.info("Configure route for {}", path);
        routeMatcher.post(path, requestHandler);
        routeMatcher.get("/status", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest request) {
                request.response().end("Ok");
            }
        });
        HttpServer httpServer = vertx.createHttpServer().requestHandler(routeMatcher);
        if (keystorePath != null) {
            httpServer = httpServer.setSSL(true)
                    .setKeyStorePath(keystorePath)
                    .setKeyStorePassword(keystorePassword);
        }

        return httpServer.listen(Integer.decode(port), host);
    }

    public HttpClient getHttpClient(URL url) {
        HttpClient httpClient = vertx.createHttpClient().setKeepAlive(false).setMaxPoolSize(20).setHost(url.getHost());

        if (keystorePath != null) {
            httpClient = httpClient.setSSL(true)
                    .setKeyStorePath(keystorePath)
                    .setKeyStorePassword(keystorePassword);
        }

        if (truststorePath != null) {
            httpClient = httpClient.setSSL(true)
                    .setTrustStorePath(truststorePath)
                    .setTrustStorePassword(truststorePassword);
        }
        if (trustAll) {
            httpClient = httpClient.setTrustAll(trustAll);
        }


        return httpClient.setPort(url.getPort());
    }
}
