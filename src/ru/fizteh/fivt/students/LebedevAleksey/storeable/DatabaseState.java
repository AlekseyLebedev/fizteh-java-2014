package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class DatabaseState extends ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.InterpreterState {
    protected Database database;

    public DatabaseState() {
        //TODO
        throw new NotImplementedException();
        String directoryPath = System.getProperty("fizteh.db.dir");
        if (directoryPath == null) {
            //throw ...
        } else {
            database = new Database(directoryPath);
        }
    }
}
