package com.ncc.kairos.moirai.clotho.exceptions;

/**
 * This class is used to signal errors when constructing example or template
 * responses to requests. Note, this is primarily used by ApiUtil.
 * 
 * @author Lawerence E. Mize, Jr.
 * @see com.ncc.kairos.moirai.clotho.api.ApiUtil
 */
public class ExampleResponseException extends RuntimeException {

    /**
     * Version UID for serialization.
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs an exception with a given message.
     * @param errorMessage an easy to read description of the error
     */
    public ExampleResponseException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Constructs an exception with a given message and maintains the generating throwable.
     * @param errorMessage an easy to read description of the error
     * @param err a throwable that captures the stacktrace
     */
    public ExampleResponseException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
