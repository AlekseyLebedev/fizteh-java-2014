package ru.fizteh.fivt.students.LebedevAleksey.storeable.json;

public class BrokenJsonException extends Exception {
    private int offsetError;

    public BrokenJsonException(String message, int offsetError) {
        super(message);
        this.offsetError = offsetError;
    }

    public BrokenJsonException(String message, Throwable cause, int offsetError) {
        super(message, cause);
        this.offsetError = offsetError;
    }

    public int getOffsetError() {
        return offsetError;
    }
}
