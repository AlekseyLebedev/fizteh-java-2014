package ru.fizteh.fivt.students.torunova.storeable.database.actions;

import ru.fizteh.fivt.students.torunova.storeable.database.TableHolder;

import java.io.IOException;

/**
 * Created by nastya on 01.11.14.
 */
public class Rollback extends Action {
    TableHolder currentTable;
    public Rollback(TableHolder currentTable) {
        this.currentTable = currentTable;
    }
    @Override
    public boolean run(String args)
            throws IOException {
        String[] parameters = parseArguments(args);
        if (checkNumberOfArguments(0, parameters.length)) {
            if (currentTable.get() == null) {
                System.err.println("no table");
                return false;
            }
            System.out.println(currentTable.get().rollback());
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return "rollback";
    }
}
