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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class StoreableTable implements ru.fizteh.fivt.storage.structured.Table, AutoCloseable {
    private final String name;
    private final Database database;
    private final ThreadLocal<Map<String, String>> changedKeys = new ThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> initialValue() {
            return new TreeMap<>();
        }
    };
    private Table stringTable;
    private List<Class<?>> columnTypes;
    private ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private AtomicBoolean closed = new AtomicBoolean(false);

    public StoreableTable(String name, Database databaseParent, List<Class<?>> types) {
        lock.writeLock().lock();
        this.name = name;
        database = databaseParent;
        columnTypes = types;
        stringTable = new Table(name, databaseParent.getRootDirectoryPath());
        lock.writeLock().unlock();
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
            changedKeys.get().remove(key);
        } else {
            changedKeys.get().put(key, value);
        }
        return oldValue;
    }

    private void checkClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Table is closed");
        }
    }

    private String getString(String key) throws LoadOrSaveException, DatabaseFileStructureException {
        if (changedKeys.get().containsKey(key)) {
            return changedKeys.get().get(key);
        } else {
            return stringTable.get(key);
        }
    }

    @Override
    public Storeable put(String key, Storeable value) throws ColumnFormatException {
        checkClosed();
        checkKeyValueNotNull(key, value);
        lock.readLock().lock();
        String result;
        try {
            result = putStrings(key, database.serialize(this, value));
        } catch (DatabaseFileStructureException | LoadOrSaveException e) {
            throw new DatabaseException(e);
        } finally {
            lock.readLock().unlock();
        }
        if (result == null) {
            return null;
        } else {
            try {
                return database.deserialize(this, result);
            } catch (ParseException e) {
                throw new ColumnFormatException(e);
            }
        }
    }

    @Override
    public Storeable remove(String key) {
        checkClosed();
        checkKeyNotNull(key);
        String value;
        lock.readLock().lock();
        try {
            value = stringTable.get(key);
            Storeable oldValue = get(key);
            if (value != null) {
                if (oldValue != null) {
                    changedKeys.get().put(key, null);
                }
            } else {
                changedKeys.get().remove(key);
            }
            return oldValue;
        } catch (LoadOrSaveException | DatabaseFileStructureException e) {
            throw new DatabaseException(e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        checkClosed();
        try {
            int deletedCount = 0;
            int addedCount = 0;
            for (String key : changedKeys.get().keySet()) {
                String value = changedKeys.get().get(key);
                if (value == null) {
                    ++deletedCount;
                } else {
                    if (stringTable.get(key) == null) {
                        addedCount++;
                    }
                }
            }
            lock.readLock().lock();
            return stringTable.count() + addedCount - deletedCount;
        } catch (LoadOrSaveException | DatabaseFileStructureException e) {
            throw new DatabaseException(e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int commit() throws IOException {
        checkClosed();
        lock.writeLock().lock();
        int changes = changesCount();
        try {
            for (String key : changedKeys.get().keySet()) {
                String value = changedKeys.get().get(key);
                if (value == null) {
                    stringTable.remove(key);
                } else {
                    stringTable.put(key, value);
                }
            }
            changedKeys.get().clear();
            stringTable.save();
        } catch (DatabaseFileStructureException | LoadOrSaveException e) {
            Database.throwIOException(e);
        } finally {
            lock.writeLock().unlock();
        }
        return changes;
    }

    public int changesCount() {
        checkClosed();
        return changedKeys.get().size();
    }

    @Override
    public int rollback() {
        checkClosed();
        int changes = changesCount();
        changedKeys.get().clear();
        return changes;
    }

    @Override
    public int getNumberOfUncommittedChanges() {
        return changesCount();
    }

    @Override
    public String getName() {
        checkClosed();
        lock.readLock().lock();
        String name = this.name;
        lock.readLock().unlock();
        return name;

    }

    @Override
    public Storeable get(String key) {
        checkClosed();
        checkKeyNotNull(key);
        lock.readLock().lock();
        String result;
        try {
            result = getString(key);
        } catch (LoadOrSaveException | DatabaseFileStructureException e) {
            throw new DatabaseException(e);
        } finally {
            lock.readLock().unlock();
        }
        try {
            if (result == null) {
                return null;
            } else {
                return database.deserialize(this, result);
            }
        } catch (ParseException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int getColumnsCount() {
        checkClosed();
        lock.readLock().lock();
        int size = columnTypes.size();
        lock.readLock().unlock();
        return size;
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        checkClosed();
        lock.readLock().lock();
        Class<?> result = columnTypes.get(columnIndex);
        lock.readLock().unlock();
        return result;
    }

    public void drop() throws DatabaseFileStructureException, LoadOrSaveException {
        checkClosed();
        lock.writeLock().lock();
        try {
            Path signatureFile = database.getRootDirectoryPath().resolve(getName()).
                    resolve(Database.TABLE_SIGNATURE_FILE_NAME);
            if (!signatureFile.toFile().delete()) {
                throw new LoadOrSaveException("Can't delete signature file for table " + getName());
            }
            stringTable.drop();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<String> list() {
        checkClosed();
        lock.readLock().lock();
        Set<String> items;
        try {
            items = new TreeSet<>(stringTable.list());
        } catch (DatabaseFileStructureException | LoadOrSaveException e) {
            throw new DatabaseException(e);
        } finally {
            lock.readLock().unlock();
        }
        for (String key : changedKeys.get().keySet()) {
            String value = changedKeys.get().get(key);
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
    }

    @Override
    public void close() throws Exception {
        checkClosed();
        Throwable rollbackException = null;
        try {
            rollback();
        } catch (Throwable e) {
            rollbackException = e;
        } finally {
            try {
                database.reloadTable(this);
                if (rollbackException != null) {
                    Exception e = (Exception) rollbackException;
                    rollbackException = null;
                    throw e;
                }
            } catch (Exception e) {
                if (rollbackException != null) {
                    e.addSuppressed(rollbackException);
                }
            } finally {
                closed.set(true);
            }
        }
    }

    @Override
    public String toString() {
        checkClosed();
        return getClass().getSimpleName() + "[" + database.getRootDirectoryPath().resolve(name).toString() + "]";
    }
}
