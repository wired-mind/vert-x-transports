package com.nxttxn.axis2.transport.vertx;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.base.AbstractTransportSender;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.io.output.WriterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxSender extends AbstractTransportSender {
    private Logger log = LoggerFactory.getLogger(VertxSender.class);

    private VertxConnectionFactoryManager connectionFactoryManager;
    private Hashtable<String, String> properties = new Hashtable<String, String>();

    @Override
    public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOutDescription) throws AxisFault {
        super.init(cfgCtx, transportOutDescription);
        connectionFactoryManager = new VertxConnectionFactoryManager(transportOutDescription);
        log.info("Vertx transport sender initialized....");
    }

    @Override
    public void sendMessage(final MessageContext messageContext, String targetEPR, OutTransportInfo outTransportInfo) throws AxisFault {
        if (messageContext.getProperty(HttpAxis2ServerHandler.RESPONSE) != null) {
            respondWith(messageContext, targetEPR);
        } else {
            final MessageContext responseMessageContext = createResponseMessageContext(messageContext);

            properties = BaseUtils.getEPRProperties(targetEPR);
            final Handler<Throwable> exceptionHandler = new Handler<Throwable>() {
                @Override
                public void handle(Throwable e) {
                    log.error("Failed to send axis2 message", e);
                    invokeSenderErrorCallback(messageContext, new Exception(e));
                }
            };
            final URL url;
            try {
                url = new URL(targetEPR);
            } catch (MalformedURLException e) {
                throw AxisFault.makeFault(e);
            }
            final AxisConfiguration axisConfiguration = messageContext.getConfigurationContext().getAxisConfiguration();
            final TransportOutDescription transportOut = axisConfiguration.getTransportOut(url.getProtocol());
            //use the protocol to load the http client. The host is set in axis2 and won't change. We'll use the
            //path from the url though.
            VertxConnectionFactory vertxConnectionFactory = new VertxConnectionFactory(transportOut);
            final HttpClient httpClient = vertxConnectionFactory.getHttpClient(url);
            final String protocalString = url.getProtocol();
            log.debug("[Vertx Axis2 Transport] [Request] [Protocol] " + protocalString);
            if(protocalString.equalsIgnoreCase("https")) {
                httpClient.setSSL(true);
            }
            //we'll want to cache these clients


            final String path = url.getPath();
            HttpClientRequest httpClientRequest = httpClient.post(path, new Handler<HttpClientResponse>() {
                @Override
                public void handle(final HttpClientResponse httpClientResponse) {
                    httpClientResponse.exceptionHandler(exceptionHandler);
                    httpClientResponse.endHandler(new Handler<Void>() {
                        @Override
                        public void handle(final Void event) {
                            log.debug("[Vertx Axis2 Transport] [END HANDLER]" + event.toString());
                        }
                    });
                    httpClientResponse.bodyHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(Buffer buffer) {
                            log.debug("[Vertx Axis2 Transport] [Response] Body: {}", buffer.toString());
                            for (Map.Entry<String, String> entry : httpClientResponse.headers().entries()) {
                                log.debug("[Vertx Axis2 Transport] [Response] Header: {} {}", entry.getKey(), entry.getValue());

                            }

                            final String contentType = httpClientResponse.headers().get("content-type");
                            final String soapAction = httpClientResponse.headers().get(BaseConstants.SOAPACTION.toLowerCase());
                            try {
                                VertxUtils.setSOAPEnvelope(buffer, responseMessageContext, contentType);
                                final MessageReceiver messageReceiver = messageContext.getAxisOperation().getMessageReceiver();
                                responseMessageContext.addRelatesTo(new RelatesTo(messageContext.getMessageID()));
                                messageReceiver.receive(responseMessageContext);
                            } catch (Exception e) {
                                log.error("Unable to process response for axis2 client", e);
                                invokeSenderErrorCallback(messageContext, e);
                            }
                            //invokeSenderCallback(messageContext, responseMessageContext);
                        }
                    });
                }
            });
//            final String contentType = getContentType(messageContext);
            final String contentType = "text/xml;charset=UTF-8"; //Not sure the right way to infer this in axis2 yet. Hard code for now.
            httpClientRequest.exceptionHandler(exceptionHandler);
            httpClientRequest.setTimeout(60000);

            final Buffer buffer = getBodyBuffer(messageContext);
            String soapAction = String.format("\"%s\"", messageContext.getSoapAction() != null ? messageContext.getSoapAction() : "");
            log.info(String.format("[Vertx Axis2 Transport] [Request] [%s - %s]. Warning, contentType is hard coded right now !!!", path, contentType));
            log.debug(String.format("[Vertx Axis2 Transport] [Request] [%s] - Request body: %s", path, buffer.toString()));
            log.debug("[Vertx Axis2 Transport] [Request] SOAPAction: {}", soapAction);
            log.debug("[Vertx Axis2 Transport] [Request] [BUFFER]: {}", buffer.toString());
            
            httpClientRequest = httpClientRequest.putHeader("Content-Type", contentType)
                    .putHeader("Content-Length", String.valueOf(buffer.length()))
                    .putHeader("SOAPAction", soapAction)
                    .putHeader("User-Agent", "Apache-HttpClient/4.1.1 (java 1.5)");

            if (buffer.length() == 0) {
                httpClientRequest.end();
            } else {
                httpClientRequest.end(buffer);
            }
        }


    }

    private void invokeSenderErrorCallback(MessageContext messageContext, Exception e) {
        Handler<Exception> callback = (Handler<Exception>) messageContext.getProperty(VertxServiceClient.ERROR_CALLBACK);
        callback.handle(e);
    }

    private void invokeSenderCallback(MessageContext messageContext, MessageContext responseMessageContext) {
        Handler<MessageContext> callback = (Handler<MessageContext>) messageContext.getProperty(VertxServiceClient.SUCCESS_CALLBACK);
        callback.handle(responseMessageContext);
    }

    private String getContentType(MessageContext messageContext) {
        return (String) messageContext.getProperty(Constants.Configuration.MESSAGE_TYPE);
    }

    private Buffer getBodyBuffer(MessageContext messageContext) {
        final byte[] body = createBody(messageContext);
        return new Buffer(body == null ? new byte[0] : body);
    }

    private void respondWith(MessageContext messageContext, String targetEPR) {
        final HttpServerResponse response = (HttpServerResponse) messageContext.getProperty(HttpAxis2ServerHandler.RESPONSE);
        final Buffer buffer = getBodyBuffer(messageContext);
        final String contentType = getContentType(messageContext);
        log.info(String.format("[Vertx Axis2 Transport] [Reply] [%s]", targetEPR));
        log.debug(String.format("[Vertx Axis2 Transport] [Reply] [%s] - Request body: %s", targetEPR, buffer.toString()));

        if (contentType != null) {
            response.putHeader("Content-Type", contentType);
        }

        response.setChunked(true); // or set Content-length
        response.setStatusMessage("OK");    //should be tied to statuscode probably

        response.setStatusCode(200);
        log.info(String.format("Http result handler ready to respond with status: %s", response.getStatusCode()));
        response.end(buffer);

    }

    private byte[] createBody(MessageContext messageContext) {
        OMOutputFormat format = BaseUtils.getOMOutputFormat(messageContext);
        MessageFormatter messageFormatter = null;
        try {
            messageFormatter = MessageProcessorSelector.getMessageFormatter(messageContext);
        } catch (AxisFault axisFault) {
            throw new AxisVertxException("Unable to get the message formatter to use");
        }

        String contentType = messageFormatter.getContentType(
                messageContext, format, messageContext.getSoapAction());

        OutputStream out;
        StringWriter sw = new StringWriter();
        try {
            out = new WriterOutputStream(sw, format.getCharSetEncoding());
        } catch (UnsupportedCharsetException ex) {
            throw new AxisVertxException("Unsupported encoding " + format.getCharSetEncoding(), ex);
        }

        try {
            messageFormatter.writeTo(messageContext, format, out, true);
            out.close();
        } catch (IOException e) {
            throw new AxisVertxException("IO Error while creating BytesMessage", e);
        }

        return sw.toString().getBytes();
    }
}