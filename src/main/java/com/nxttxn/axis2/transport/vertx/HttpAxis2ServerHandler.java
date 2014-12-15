package com.nxttxn.axis2.transport.vertx;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

/**
* Created with IntelliJ IDEA.
* User: chuck
* Date: 7/16/13
* Time: 3:50 PM
* To change this template use File | Settings | File Templates.
*/
public class HttpAxis2ServerHandler implements Handler<HttpServerRequest> {
    public static final String RESPONSE = "VERTX_RESPONSE";
    private Logger log = LoggerFactory.getLogger(HttpAxis2ServerHandler.class);
    private final VertxEndpoint vertxEndpoint;


    public HttpAxis2ServerHandler(VertxEndpoint vertxEndpoint) {

        this.vertxEndpoint = vertxEndpoint;
    }

    @Override
    public void handle(final HttpServerRequest httpServerRequest) {
        httpServerRequest.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                //build the message and hand it over to axisEngine
                MessageContext messageContext = null;
                try {
                    messageContext = vertxEndpoint.createMessageContext();
                    messageContext.setProperty(HttpAxis2ServerHandler.RESPONSE, httpServerRequest.response());
                } catch (AxisFault axisFault) {
                    log.error("Failed to create axis2 message context", axisFault);
                }
                final String contentType = httpServerRequest.headers().get("content-type");
                try {
                    VertxUtils.setSOAPEnvelope(buffer, messageContext, contentType);
                    AxisEngine.receive(messageContext);
                } catch (AxisFault axisFault) {
                    log.error("Unable to process http request with axis2", axisFault);
                }
            }
        });

    }
}
