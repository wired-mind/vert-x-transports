package com.nxttxn.axis2.transport.vertx;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.vertx.java.core.Handler;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/18/13
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxServiceClient extends ServiceClient {
    public static final String SUCCESS_CALLBACK = "SUCCESS_CALLBACK";
    public static final String ERROR_CALLBACK = "ERROR_CALLBACK";
    private QName operationQName;

    public VertxServiceClient(ConfigurationContext configContext, AxisService axisService) throws AxisFault {
        super(configContext, axisService);
    }

    public VertxServiceClient(ConfigurationContext configContext, Definition wsdl4jDefinition, QName wsdlServiceName, String portName) throws AxisFault {
        super(configContext, wsdl4jDefinition, wsdlServiceName, portName);
    }

    public VertxServiceClient(ConfigurationContext configContext, URL wsdlURL, QName wsdlServiceName, String portName) throws AxisFault {
        super(configContext, wsdlURL, wsdlServiceName, portName);
    }

    public VertxServiceClient() throws AxisFault {
        super();
    }


    public void sendWithVertx(OMElement xmlPayload, final AxisCallback callback) throws AxisFault {

        MessageContext mc = new MessageContext();
        fillSOAPEnvelope(mc, xmlPayload);
        operationQName = xmlPayload.getQName();
        final String soapAction = getAxisService().getOperation(operationQName).getSoapAction();
        getOptions().setAction(soapAction);
        OperationClient mepClient = createClient(operationQName);

        //Set this flag to prevent use of a new thread
        mc.setProperty(Constants.Configuration.USE_ASYNC_OPERATIONS, Boolean.TRUE);
        mc.setProperty(VertxServiceClient.ERROR_CALLBACK, new Handler<Exception>() {
            @Override
            public void handle(Exception e) {
                callback.onError(e);
            }
        });
        mepClient.setCallback(callback);
        mepClient.addMessageContext(mc);

        mepClient.execute(false);


    }



    /**
     * Prepare a SOAP envelope with the stuff to be sent.
     *
     * @param messageContext the message context to be filled
     * @param xmlPayload     the payload content
     * @throws AxisFault if something goes wrong
     */
    private void fillSOAPEnvelope(MessageContext messageContext, OMElement xmlPayload)
            throws AxisFault {
        messageContext.setServiceContext(getServiceContext());
        SOAPFactory soapFactory = getSOAPFactory();
        SOAPEnvelope envelope = soapFactory.getDefaultEnvelope();
        if (xmlPayload != null) {
            envelope.getBody().addChild(xmlPayload);
        }
        addHeadersToEnvelope(envelope);
        messageContext.setEnvelope(envelope);
    }

    /**
     * Return the SOAP factory to use depending on what options have been set. If the SOAP version
     * can not be seen in the options, version 1.1 is the default.
     *
     * @return the SOAP factory
     * @see org.apache.axis2.client.Options#setSoapVersionURI(String)
     */
    private SOAPFactory getSOAPFactory() {
        String soapVersionURI = getOptions().getSoapVersionURI();
        if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(soapVersionURI)) {
            return OMAbstractFactory.getSOAP12Factory();
        } else {
            // make the SOAP 1.1 the default SOAP version
            return OMAbstractFactory.getSOAP11Factory();
        }
    }
}
