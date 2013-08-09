package com.nxttxn.axis2.transport.vertx;

import org.apache.axiom.om.OMElement;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/19/13
 * Time: 10:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class Axis2CallbackResult {
    private final OMElement responsePayload;
    private final Throwable exception;

    public Axis2CallbackResult(OMElement responsePayload) {

        this.responsePayload = responsePayload;
        exception = null;
    }

    public Axis2CallbackResult(Exception exception) {

        this.exception = exception;
        responsePayload = null;
    }

    public OMElement getResponsePayload() {
        return responsePayload;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean failed() {
        return getException() != null;
    }
}
