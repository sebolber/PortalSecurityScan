package com.ahs.cvm.application.alert;

/** Wirft der Mail-Adapter, wenn der Versand fehlschlaegt. */
public class MailDispatchException extends RuntimeException {

    public MailDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
