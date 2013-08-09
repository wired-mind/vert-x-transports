package com.nxttxn.axis2.transport.vertx;

import org.apache.axis2.AxisFault;

import java.nio.charset.UnsupportedCharsetException;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/16/13
 * Time: 12:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class AxisVertxException extends RuntimeException {
    public AxisVertxException(Throwable cause) {
        super(cause);
    }

    public AxisVertxException(String message) {
        super(message);
    }

    public AxisVertxException(String message, Throwable cause) {
        super(message, cause);
    }
}
