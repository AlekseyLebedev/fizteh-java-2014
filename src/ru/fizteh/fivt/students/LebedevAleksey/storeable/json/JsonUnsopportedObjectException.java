package ru.fizteh.fivt.students.LebedevAleksey.storeable.json;

public class JsonUnsopportedObjectException extends Exception {
    public JsonUnsopportedObjectException() {
    }

    public JsonUnsopportedObjectException(String message) {
        super(message);
    }

    public JsonUnsopportedObjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonUnsopportedObjectException(Throwable cause) {
        super(cause);
    }
}
