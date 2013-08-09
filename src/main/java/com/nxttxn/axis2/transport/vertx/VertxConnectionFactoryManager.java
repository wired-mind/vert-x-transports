package com.nxttxn.axis2.transport.vertx;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 12:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxConnectionFactoryManager {

    private final ParameterInclude trpDesc;
    private Logger log = LoggerFactory.getLogger(VertxConnectionFactoryManager.class);


    public VertxConnectionFactoryManager(ParameterInclude trpDesc) {
        this.trpDesc = trpDesc;

    }




    public VertxConnectionFactory getVertxConnectionFactory() {
        return new VertxConnectionFactory(trpDesc);
    }

    public void addParameter(String key, Object value) throws AxisFault {
        trpDesc.addParameter(new Parameter(key, value));
    }
}
