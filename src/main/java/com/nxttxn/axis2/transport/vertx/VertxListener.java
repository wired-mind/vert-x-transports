package com.nxttxn.axis2.transport.vertx;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.transport.base.AbstractTransportListenerEx;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 10:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class VertxListener extends AbstractTransportListenerEx<VertxEndpoint> {
    public static final String PATH = "VertxAxis2ServicePath";
    private VertxConnectionFactoryManager connectionFactoryManager;

    @Override
    protected void doInit() throws AxisFault {
        connectionFactoryManager = new VertxConnectionFactoryManager(getTransportInDescription());
        log.info("Vertx Transport Receiver/Listener initialized...");
    }

    @Override
    protected VertxEndpoint createEndpoint() {
        return new VertxEndpoint(this);
    }

    @Override
    protected void startEndpoint(VertxEndpoint endpoint) throws AxisFault {
        endpoint.registerHandler(getConfigurationContext());
    }

    @Override
    protected void stopEndpoint(VertxEndpoint endpoint) {
        endpoint.unregisterHandler();
    }

    public VertxConnectionFactory getConnectionFactory(AxisService service) throws AxisFault {

        connectionFactoryManager.addParameter(PATH, service.getParameter(PATH).getValue());
        connectionFactoryManager.addParameter(VertxUtils.VERTX, service.getParameter(VertxUtils.VERTX).getValue());
        return connectionFactoryManager.getVertxConnectionFactory();

    }
}
