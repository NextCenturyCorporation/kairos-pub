package com.ncc.kairos.moirai.clotho.exceptions;

/**
 * This class is used to signal exceptions graph drivers and tinkerpop connectors
 * to various graph dbs.
 * 
 * @author Lawerence E. Mize, Jr.
 */
public class DriverException extends RuntimeException {

    /**
     * Version UID for serialization.
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs an exception with a given message.
     * @param errorMessage an easy to read description of the error
     */
    public DriverException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Constructs an exception with a given message and maintains the generating throwable.
     * @param errorMessage an easy to read description of the error
     * @param err a throwable that captures the stacktrace
     */
    public DriverException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

}
