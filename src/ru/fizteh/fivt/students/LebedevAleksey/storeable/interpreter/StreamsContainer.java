package ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter;

import java.io.InputStream;
import java.io.PrintStream;

public class StreamsContainer {

    private final PrintStream out;
    private final PrintStream err;
    private final InputStream in;

    public StreamsContainer(PrintStream out, PrintStream err, InputStream in) {
        this.out = out;
        this.err = err;
        this.in = in;
    }


    public StreamsContainer(){
        out = System.out;
        err = System.err;
        in = System.in;
    }

    public PrintStream getOut() {
        return out;
    }

    public PrintStream getErr() {
        return err;
    }

    public InputStream getIn() {
        return in;
    }
}
