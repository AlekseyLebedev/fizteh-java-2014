package ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter;

public class InterpreterState {
    private boolean run = true;

    public boolean exited() {
        return !run;
    }

    public void exit() {
        run = false;
    }
}
