package com.ncc.kairos.moirai.clotho.exceptions;

/**
 * This class is used to signal exceptions in validating graphs within Clotho.
 * Specifically, this is used by various utility methods in the processing
 * of KAIROS JSON-LD documents.
 * 
 * @author Lawerence E. Mize, Jr.
 */
public class EmptyContextElementException extends RuntimeException {

    /**
     * Version UID for serialization.
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs an exception with a given message.
     * @param errorMessage an easy to read description of the error
     */
    public EmptyContextElementException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Constructs an exception with a given message and maintains the generating throwable.
     * @param errorMessage an easy to read description of the error
     * @param err a throwable that captures the stacktrace
     */
    public EmptyContextElementException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

}
