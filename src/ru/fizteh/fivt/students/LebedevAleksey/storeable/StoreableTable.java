package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.DatabaseFileStructureException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.LoadOrSaveException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.Table;
import ru.fizteh.fivt.students.LebedevAleksey.junit.DatabaseException;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.function.Consumer;

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
        stringTable = new Table(name, databaseParent.getRootDirectoryPath());
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
            String result = putStrings(key, database.serialize(this, value));
            if (result == null) {
                return null;
            } else {
                return database.deserialize(this, result);
            }
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
            Database.throwIOException(e);
        }
        return changes;
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
    public int getNumberOfUncommittedChanges() {
        return changesCount();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Storeable get(String key) {
        checkKeyNotNull(key);
        try {
            String result = getString(key);
            if (result == null) {
                return null;
            } else {
                return database.deserialize(this, result);
            }
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
        Path signatureFile = database.getRootDirectoryPath().resolve(getName()).
                resolve(Database.TABLE_SIGNATURE_FILE_NAME);
        if (!signatureFile.toFile().delete()) {
            throw new LoadOrSaveException("Can't delete signature file for table " + getName());
        }
        stringTable.drop();
    }

    @Override
    public List<String> list() {
        try {
            Set<String> items = new TreeSet<>(stringTable.list());
            for (String key : changedKeys.keySet()) {
                String value = changedKeys.get(key);
                if (value == null) {
                    items.remove(key);
                } else {
                    items.add(key);
                }
            }
            final ArrayList<String> result = new ArrayList<>(items.size());
            items.forEach(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    result.add(s);
                }
            });
            return result;
        } catch (DatabaseFileStructureException | LoadOrSaveException e) {
            throw new DatabaseException(e);
        }
    }
}
