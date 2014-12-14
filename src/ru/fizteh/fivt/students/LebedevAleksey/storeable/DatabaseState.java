package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.DatabaseFileStructureException;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter.InterpreterState;

import java.io.IOException;

public class DatabaseState extends InterpreterState {
    private Database database;
    private StoreableTable table;

    public StoreableTable getCurrentTable() {
        return table;
    }

    public void setCurrentTable(StoreableTable table) {
        this.table = table;
    }

    public Database getDatabase() {
        return database;
    }


    public DatabaseState() throws IOException, DatabaseFileStructureException {
        String directoryPath = System.getProperty("fizteh.db.dir");
        if (directoryPath == null) {
            throw new DatabaseFileStructureException("Database directory doesn't set");
        } else {
            database = new Database(directoryPath);
        }
    }
}
