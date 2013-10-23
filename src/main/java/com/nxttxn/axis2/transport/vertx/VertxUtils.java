package com.nxttxn.axis2.transport.vertx;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.builder.SOAPBuilder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.TransportUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;

import java.io.ByteArrayInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 3:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxUtils {
    public static final String VERTX = "VERTX";
    private static Logger log = LoggerFactory.getLogger(VertxUtils.class);

    public static void setSOAPEnvelope(Buffer buffer, MessageContext messageContext, String contentType) throws AxisFault {
        if (contentType == null) {
            contentType = "text/xml";         // TODO: hardcoding for the SOAP11 builder
        }

        int index = contentType.indexOf(';');
        String type = index > 0 ? contentType.substring(0, index) : contentType;
        Builder builder = BuilderUtil.getBuilderFromSelector(type, messageContext);
        if (builder == null) {
            if (log.isDebugEnabled()) {
                log.debug("No message builder found for type '" + type + "'. Falling back to SOAP.");
            }
            builder = new SOAPBuilder();
        }
        messageContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, "UTF-8");
        OMElement documentElement = null;

        byte[] bytes = buffer.getBytes();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        documentElement = builder.processDocument(byteArrayInputStream, contentType, messageContext);


        messageContext.setEnvelope(TransportUtils.createSOAPEnvelope(documentElement));


    }
}
