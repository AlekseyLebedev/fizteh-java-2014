package ru.fizteh.fivt.students.LebedevAleksey.storeable.json;

public class JsonUnsupportedObjectException extends Exception {
    public JsonUnsupportedObjectException() {
    }

    public JsonUnsupportedObjectException(String message) {
        super(message);
    }

    public JsonUnsupportedObjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonUnsupportedObjectException(Throwable cause) {
        super(cause);
    }
}
