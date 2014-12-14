package ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter;

public class ArgumentException extends ParserException {
    public ArgumentException(String message) {
        super(message);
    }

    public ArgumentException(String message, Throwable ex) {
        super(message, ex);
    }
}
