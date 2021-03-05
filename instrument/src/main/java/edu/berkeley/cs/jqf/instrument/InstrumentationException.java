package edu.berkeley.cs.jqf.instrument;

public class InstrumentationException extends RuntimeException {
    public InstrumentationException(String msg) {
        super(msg);
    }

    public InstrumentationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
