package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.DatabaseFileStructureException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.LoadOrSaveException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.Table;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.function.Consumer;

public class StoreableTable implements ru.fizteh.fivt.storage.structured.Table {
    private Map<String, String> changedKeys = new TreeMap<>();

    private Table stringTable;

    public StoreableTable(String name, Database databaseParent) {
        Table stringTable=new Table(name, databaseParent.getRootDirectoryPath());
    }

    @Override
    public Storeable put(String key, Storeable value) throws ColumnFormatException {
        return null;
    }

    @Override
    public boolean remove(String key) throws LoadOrSaveException, DatabaseFileStructureException {
        String value = super.get(key);
        String oldValue = get(key);
        if (value != null) {
            if (oldValue != null) {
                changedKeys.put(key, null);
            }
        } else {
            changedKeys.remove(key);
        }
        return (oldValue != null);
    }

    @Override
    public int size() {
        return 0;
    }

    public String getAndRemove(String key) throws DatabaseFileStructureException, LoadOrSaveException {
        String oldValue = get(key);
        remove(key);
        return oldValue;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String get(String key) throws LoadOrSaveException, DatabaseFileStructureException {
        if (changedKeys.containsKey(key)) {
            return changedKeys.get(key);
        } else {
            return super.get(key);
        }
    }

    @Override
    public String put(String key, String value) throws LoadOrSaveException, DatabaseFileStructureException {
        String oldValue = get(key);
        if (value.equals(super.get(key))) {
            changedKeys.remove(key);
        } else {
            changedKeys.put(key, value);
        }
        return oldValue;
    }

    @Override
    public void save() throws LoadOrSaveException, DatabaseFileStructureException {
        for (String key : changedKeys.keySet()) {
            String value = changedKeys.get(key);
            if (value == null) {
                super.remove(key);
            } else {
                super.put(key, value);
            }
        }
        changedKeys.clear();
        super.save();
    }

    @Override
    public int count() throws LoadOrSaveException, DatabaseFileStructureException {
        int deletedCount = 0;
        int addedCount = 0;
        for (String key : changedKeys.keySet()) {
            String value = changedKeys.get(key);
            if (value == null) {
                ++deletedCount;
            } else {
                if (super.get(key) == null) {
                    addedCount++;
                }
            }
        }
        return super.count() + addedCount - deletedCount;
    }

    @Override
    public ArrayList<String> list() throws LoadOrSaveException, DatabaseFileStructureException {
        Set<String> items = new TreeSet<>(super.list());
        for (String key : changedKeys.keySet()) {
            String value = changedKeys.get(key);
            if (value == null) {
                items.remove(key);
            } else {
                items.add(key);
            }
        }
        ArrayList<String> result = new ArrayList<>(items.size());
        items.forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                result.add(s);
            }
        });
        return result;
    }

    public int changesCount() {
        return changedKeys.size();
    }

    public int commit(){
        int changes = changesCount();
        save();
        return changes;
    }

    public int rollback() {
        int changes = changesCount();
        changedKeys.clear();
        initParts();
        return changes;
    }

    @Override
    public int getColumnsCount() {
        return 0;
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        return null;
    }

    public void drop() {
        //TODO
        throw new NotImplementedException();
    }
}
