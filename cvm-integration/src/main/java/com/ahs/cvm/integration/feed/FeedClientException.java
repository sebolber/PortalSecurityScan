package com.ahs.cvm.integration.feed;

/** Rueckfallfehler fuer alle Feed-Adapter. */
public class FeedClientException extends RuntimeException {

    private final FeedSource source;

    public FeedClientException(FeedSource source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public FeedClientException(FeedSource source, String message) {
        super(message);
        this.source = source;
    }

    public FeedSource source() {
        return source;
    }
}
