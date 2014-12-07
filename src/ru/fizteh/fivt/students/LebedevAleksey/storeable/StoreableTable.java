package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.DatabaseFileStructureException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.LoadOrSaveException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.Table;
import ru.fizteh.fivt.students.LebedevAleksey.junit.DatabaseException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StoreableTable implements ru.fizteh.fivt.storage.structured.Table {
    private final String name;
    private final Database database;
    private Map<String, String> changedKeys = new TreeMap<>();
    private Table stringTable;
    private List<Class<?>> columnTypes;

    public StoreableTable(String name, Database databaseParent, List<Class<?>> types) {
        this.name = name;
        database = databaseParent;
        columnTypes = types;
        Table stringTable = new Table(name, databaseParent.getRootDirectoryPath());
    }

    private void checkKeyNotNull(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Argument \"key\" is null");
        }
    }

    private void checkKeyValueNotNull(String key, Storeable value) {
        checkKeyNotNull(key);
        if (value == null) {
            throw new IllegalArgumentException("Argument \"value\" is null");
        }
    }

    private String putStrings(String key, String value) throws DatabaseFileStructureException, LoadOrSaveException {
        String oldValue = getString(key);
        if (value.equals(stringTable.get(key))) {
            changedKeys.remove(key);
        } else {
            changedKeys.put(key, value);
        }
        return oldValue;
    }

    private String getString(String key) throws LoadOrSaveException, DatabaseFileStructureException {
        if (changedKeys.containsKey(key)) {
            return changedKeys.get(key);
        } else {
            return stringTable.get(key);
        }
    }

    @Override
    public Storeable put(String key, Storeable value) throws ColumnFormatException {
        checkKeyValueNotNull(key, value);
        try {
            return database.deserialize(this, putStrings(key, database.serialize(this, value)));
        } catch (DatabaseFileStructureException | LoadOrSaveException e) {
            throw new DatabaseException(e);
        } catch (ParseException e) {
            throw new ColumnFormatException(e);
        }
    }

    @Override
    public Storeable remove(String key) {
        checkKeyNotNull(key);
        String value;
        try {
            value = stringTable.get(key);
        } catch (LoadOrSaveException | DatabaseFileStructureException e) {
            throw new DatabaseException(e);
        }
        Storeable oldValue = get(key);
        if (value != null) {
            if (oldValue != null) {
                changedKeys.put(key, null);
            }
        } else {
            changedKeys.remove(key);
        }
        return oldValue;
    }

    @Override
    public int size() {
        try {
            int deletedCount = 0;
            int addedCount = 0;
            for (String key : changedKeys.keySet()) {
                String value = changedKeys.get(key);
                if (value == null) {
                    ++deletedCount;
                } else {
                    if (stringTable.get(key) == null) {
                        addedCount++;
                    }
                }
            }
            return stringTable.count() + addedCount - deletedCount;
        } catch (LoadOrSaveException | DatabaseFileStructureException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int commit() throws IOException {
        int changes = changesCount();
        try {
            for (String key : changedKeys.keySet()) {
                String value = changedKeys.get(key);
                if (value == null) {
                    stringTable.remove(key);
                } else {
                    stringTable.put(key, value);
                }
            }
            changedKeys.clear();
            stringTable.save();
        } catch (DatabaseFileStructureException | LoadOrSaveException e) {
            throw new IOException(e.getMessage(), e);
        }
        return 0;
    }

    public int changesCount() {
        return changedKeys.size();
    }

    @Override
    public int rollback() {
        int changes = changesCount();
        changedKeys.clear();
        return changes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Storeable get(String key) {
        checkKeyNotNull(key);
        try {
            return database.deserialize(this, getString(key));
        } catch (ParseException | LoadOrSaveException | DatabaseFileStructureException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int getColumnsCount() {
        return columnTypes.size();
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        return columnTypes.get(columnIndex);
    }

    public void drop() throws DatabaseFileStructureException, LoadOrSaveException {
        stringTable.drop();
    }
}
