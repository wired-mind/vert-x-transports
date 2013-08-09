package com.nxttxn.axis2.transport.vertx;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.transport.base.ProtocolEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServer;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class VertxEndpoint extends ProtocolEndpoint {
    private Logger log = LoggerFactory.getLogger(VertxEndpoint.class);

    private final VertxListener vertxListener;
    private VertxConnectionFactory vertxConnectionFactory;
    private HttpServer httpServer;

    public VertxEndpoint(VertxListener vertxListener) {

        this.vertxListener = vertxListener;
    }

    @Override
    public boolean loadConfiguration(ParameterInclude params) throws AxisFault {
        if (!(params instanceof AxisService)) {
            return false;
        }

        AxisService service = (AxisService)params;

        vertxConnectionFactory = vertxListener.getConnectionFactory(service);
        if (vertxConnectionFactory == null) {
            return false;
        }
        return true;
    }

    @Override
    public EndpointReference[] getEndpointReferences(AxisService service, String ip) throws AxisFault {
        return new EndpointReference[0];  //To change body of implemented methods use File | Settings | File Templates.
    }


    public void registerHandler(ConfigurationContext configurationContext) {

        final HttpAxis2ServerHandler requestHandler = new HttpAxis2ServerHandler(this);
        httpServer = vertxConnectionFactory.getHttpServer(requestHandler);

    }

    public void unregisterHandler() {

    }

}
