package org.openalex4j;

public class OpenAlexException extends RuntimeException {

    public OpenAlexException(String message) {
        super(message);
    }

    public OpenAlexException(String message, Throwable cause) {
        super(message, cause);
    }
}
