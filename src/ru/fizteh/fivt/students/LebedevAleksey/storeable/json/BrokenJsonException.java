package ru.fizteh.fivt.students.LebedevAleksey.storeable.json;

public class BrokenJsonException extends Exception {
    public BrokenJsonException() {
    }

    public BrokenJsonException(String message) {
        super(message);
    }

    public BrokenJsonException(String message, Throwable cause) {
        super(message, cause);
    }

    public BrokenJsonException(Throwable cause) {
        super(cause);
    }
}
