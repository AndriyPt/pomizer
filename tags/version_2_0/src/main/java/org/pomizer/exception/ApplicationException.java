package org.pomizer.exception;

public class ApplicationException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public ApplicationException(final String message) {
        super(message);
    }
}
