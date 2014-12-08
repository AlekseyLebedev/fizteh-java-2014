package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;


import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.students.LebedevAleksey.junit.DatabaseException;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.Database;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.StoreableTable;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class StringTableWrapper implements ru.fizteh.fivt.storage.strings.Table {

    private StoreableTable table;
    private Database database;

    private StringTableWrapper(StoreableTable table, Database database) {
        this.table = table;
        this.database = database;
    }

    public static StringTableWrapper create(ru.fizteh.fivt.storage.structured.Table table, Database database) {
        if (table == null) {
            return null;
        } else {
            return new StringTableWrapper((StoreableTable) table, database);
        }
    }


    @Override
    public String getName() {
        return table.getName();
    }

    @Override
    public String get(String key) {
        Storeable storeable = table.get(key);
        return (storeable == null ? null : (String) storeable.getColumnAt(0));
    }

    @Override
    public String put(String key, String value) {
        try {
            Storeable result;
            if (value == null) {
                result = table.put(key, null);
            } else {
                result = table.put(key, database.deserialize(table, "[\"" + value + "\"]"));
            }
            return result == null ? null : (String) result.getColumnAt(0);
        } catch (ParseException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public String remove(String key) {
        Storeable result = table.remove(key);
        return result == null ? null : (String) result.getColumnAt(0);
    }

    @Override
    public int size() {
        return table.size();
    }

    @Override
    public int commit() {
        try {
            return table.commit();
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int rollback() {
        return table.rollback();
    }

    @Override
    public List<String> list() {
        try {
            return table.list();
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
    }
}
