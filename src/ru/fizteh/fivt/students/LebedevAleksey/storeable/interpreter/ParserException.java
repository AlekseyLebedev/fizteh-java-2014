package ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter;

public class ParserException extends Exception {
    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable ex) {
        super(message, ex);
    }
}
