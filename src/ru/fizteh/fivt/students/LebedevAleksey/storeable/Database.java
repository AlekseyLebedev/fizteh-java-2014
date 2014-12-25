package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.DatabaseFileStructureException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.LoadOrSaveException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.Pair;
import ru.fizteh.fivt.students.LebedevAleksey.junit.DatabaseException;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.json.BrokenJsonException;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.json.JsonParser;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.json.JsonUnsupportedObjectException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

public class Database implements TableProvider, AutoCloseable {
    public static final String TABLE_SIGNATURE_FILE_NAME = "signature.tsv";
    private static final String INCORRECT_NAME_OF_TABLES = "This name is not correct, folder can't be created";
    private static Map<String, Class<?>> stringTypesMap = new HashMap<>();
    private static Map<Class, String> typesStringMap = new HashMap<>();
    private AtomicBoolean closed = new AtomicBoolean(false);

    static {
        stringTypesMap.put("int", Integer.class);
        stringTypesMap.put("long", Long.class);
        stringTypesMap.put("byte", Byte.class);
        stringTypesMap.put("float", Float.class);
        stringTypesMap.put("double", Double.class);
        stringTypesMap.put("boolean", Boolean.class);
        stringTypesMap.put("String", String.class);
        stringTypesMap.forEach(new BiConsumer<String, Class<?>>() {
            @Override
            public void accept(String alias, Class<?> type) {
                typesStringMap.put(type, alias);
            }
        });
    }

    private Path directoryPath;
    private Map<String, StoreableTable> tables = new HashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public Database(String directory) throws IOException {
        lock.writeLock().lock();
        try {
            assertArgumentNotNull(directory, "directory");
            File root = new File(directory);
            directoryPath = root.toPath();
            File[] tables = root.listFiles();
            for (File file : tables) {
                loadTable(file);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void throwIOException(Throwable e) throws IOException {
        if (e.getCause() != null) {
            try {
                IOException ioException = (IOException) e.getCause();
                throw ioException;
            } catch (ClassCastException wrongClass) {
                //Not IOException
            }
        }
        throw new IOException(e.getMessage(), e);
    }

    public static List<Class<?>> parseTypes(String[] input) throws ParseException {
        List<Class<?>> types = new ArrayList<>(input.length);
        for (String item : input) {
            Class<?> type = stringTypesMap.get(item);
            if (type != null) {
                types.add(type);
            } else {
                throw new ParseException("Wrong type: " + item, -1);
            }
        }
        return types;
    }

    private void checkClosed() {
        if (closed.get()) {
            throw new IllegalStateException("TableProvider is closed");
        }
    }

    private void loadTable(File file) throws IOException {
        if (file.isDirectory()) {
            File signature = file.toPath().resolve(TABLE_SIGNATURE_FILE_NAME).toFile();
            String tablename = file.getName();
            if (signature.exists() && signature.isFile()) {
                String signatureString;
                try (FileInputStream stream = new FileInputStream(signature.getAbsolutePath())) {
                    try (DataInputStream signaturedata = new DataInputStream(stream)) {
                        signatureString = signaturedata.readUTF();
                    }
                }
                String[] tokens = signatureString.split(" ");
                List<Class<?>> types = new ArrayList<>();
                if (signatureString.length() != 0) {
                    for (String item : tokens) {
                        Class<?> type = stringTypesMap.get(item);
                        if (type == null) {
                            throw new IOException("Wrong type name in signature of table " + tablename);
                        } else {
                            types.add(type);
                        }
                    }
                }
                StoreableTable table = generateTable(tablename, types);
                this.tables.put(tablename, table);
            } else {
                throw new IOException("Where is not signature file in table " + tablename);
            }
        } else {
            fileFoundInRootDirectory(file);
        }
    }

    void reloadTable(Table table) throws IOException {
        checkClosed();
        lock.writeLock().lock();
        try {
            if (tables.get(table.getName()) == table) {
                loadTable(getRootDirectoryPath().resolve(table.getName()).toFile());
            } else {
                throw new IllegalArgumentException("Table doesn't exist.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void fileFoundInRootDirectory(File file) throws IOException {
        throw new IOException("There is file " + file.getName() + " in root directory");
    }

    @Override
    public Table getTable(String name) {
        checkClosed();
        assertArgumentNotNull(name, "name");
        lock.readLock().lock();
        StoreableTable table = tables.get(name);
        lock.readLock().unlock();
        return table;
    }

    @Override
    public Table createTable(String name, List<Class<?>> columnTypes) throws IOException {
        checkClosed();
        assertArgumentNotNull(name, "name");
        assertArgumentNotNull(columnTypes, "columnTypes");
        lock.writeLock().lock();
        try {
            Table checkExists = getTable(name);
            if (checkExists == null) {
                Path rootDirectoryPath = getRootDirectoryPath();
                Path path = rootDirectoryPath.resolve(name);
                if (path.startsWith(rootDirectoryPath) && path.getParent().equals(rootDirectoryPath)) {
                    if (columnTypes == null) {
                        throw new IllegalArgumentException("Argument \"columnTypes\" is null.");
                    }
                    String tableSignature = createTableSignature(columnTypes);
                    try {
                        Files.createDirectory(path);
                        try (FileOutputStream stream = new FileOutputStream(path.resolve(
                                TABLE_SIGNATURE_FILE_NAME).toString())) {
                            try (DataOutputStream dataStream = new DataOutputStream(stream)) {
                                dataStream.writeUTF(tableSignature);
                                dataStream.flush();
                            }
                        }
                    } catch (Exception ex) {
                        try {
                            Files.delete(path);
                        } catch (Throwable suppressed) {
                            ex.addSuppressed(suppressed);
                        }
                        throw ex;
                    }
                    StoreableTable table = generateTable(name, columnTypes);
                    tables.put(name, table);
                    return table;
                } else {
                    throw new IllegalArgumentException(INCORRECT_NAME_OF_TABLES);
                }
            } else {
                return null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String createTableSignature(List<Class<?>> columnTypes) {
        StringBuilder tableSignature = new StringBuilder();
        for (Class<?> column : columnTypes) {
            if (column == null) {
                throw new IllegalArgumentException("Null column");
            }
            String type = typesStringMap.get(column);
            if (type == null) {
                throw new IllegalArgumentException("Type is not supported");
            }
            tableSignature.append(type);
            tableSignature.append(" ");
        }
        return tableSignature.deleteCharAt(tableSignature.length() - 1).toString();
    }

    public Path getRootDirectoryPath() {
        checkClosed();
        return directoryPath;
    }

    private StoreableTable generateTable(String name, List<Class<?>> types) {
        return new StoreableTable(name, this, types);
    }

    @Override
    public void removeTable(String name) throws IOException {
        checkClosed();
        assertArgumentNotNull(name, "name");
        lock.writeLock().lock();
        try {
            StoreableTable table = (StoreableTable) getTable(name);
            if (table == null) {
                throw new IllegalStateException("There is no table with name \"" + name + "\"");
            }
            try {
                table.drop();
            } catch (DatabaseFileStructureException | LoadOrSaveException e) {
                throwIOException(e);
            }
            tables.remove(name);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Storeable deserialize(Table table, String value) throws ParseException {
        checkClosed();
        assertArgumentNotNull(table, "table");
        assertArgumentNotNull(value, "value");
        List<Object> data;
        try {
            data = (List<Object>) JsonParser.parseJson(value);
        } catch (BrokenJsonException e) {
            throw new ParseException("Can't parse JSON: " + e.getMessage(), e.getOffsetError());
        } catch (ClassCastException e) {
            throw new ParseException("Wrong JSON: not a list", 0);
        }
        if (data.size() == table.getColumnsCount()) {
            Storeable storable = createFor(table);
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i) == null) {
                    storable.setColumnAt(i, null);
                } else if (data.get(i).getClass() == table.getColumnType(i)) {
                    storable.setColumnAt(i, data.get(i));
                } else if (!tryCastInteger(table, i, data.get(i), storable)
                        && !tryCastFloat(table, i, data.get(i), storable)) {
                    throw new ParseException("Wrong data type in column number " + i, -1);
                }
            }
            return storable;
        } else {
            throw new ParseException("Wrong size of list: have " + data.size() + ", table have "
                    + table.getColumnsCount() + " columns.", value.length() - 1);
        }
    }

    private boolean tryCastInteger(Table table, int column, Object value, Storeable result) {
        if (table.getColumnType(column) == Integer.class || table.getColumnType(column) == Byte.class) {
            if (value.getClass() == Long.class) {
                if (table.getColumnType(column) == Integer.class) {
                    long val = (long) value;
                    if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                        result.setColumnAt(column, new Integer((int) val));
                        return true;
                    }
                } else {
                    if (table.getColumnType(column) == Byte.class) {
                        long val = (long) value;
                        if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
                            result.setColumnAt(column, new Byte((byte) val));
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean tryCastFloat(Table table, int column, Object value, Storeable result) {
        if (table.getColumnType(column) == Float.class) {
            if (value.getClass() == Double.class) {
                if (table.getColumnType(column) == Float.class) {
                    double val = (double) value;
                    if (Math.abs(val) >= Float.MIN_VALUE && val <= Float.MAX_VALUE) {
                        result.setColumnAt(column, (float) val);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String serialize(Table table, Storeable value) throws ColumnFormatException {
        checkClosed();
        assertArgumentNotNull(table, "table");
        assertArgumentNotNull(value, "value");
        List<Object> data = new ArrayList<>();
        for (int i = 0; i < table.getColumnsCount(); i++) {
            data.add(value.getColumnAt(i));
        }
        try {
            return JsonParser.getJson(data);
        } catch (JsonUnsupportedObjectException e) {
            throw new ColumnFormatException("Unknown column format", e);
        }
    }

    @Override
    public Storeable createFor(Table table) {
        checkClosed();
        assertArgumentNotNull(table, "table");
        return new ru.fizteh.fivt.students.LebedevAleksey.storeable.Storeable(Arrays.asList(
                new Object[table.getColumnsCount()]), table);
    }

    @Override
    public Storeable createFor(Table table, List<?> values) throws ColumnFormatException, IndexOutOfBoundsException {
        checkClosed();
        assertArgumentNotNull(table, "table");
        assertArgumentNotNull(values, "values");
        return new ru.fizteh.fivt.students.LebedevAleksey.storeable.Storeable((List<Object>) values, table);
    }

    @Override
    public List<String> getTableNames() {
        checkClosed();
        lock.readLock().lock();
        final List<String> result = new ArrayList<>(tables.size());
        tables.keySet().forEach((String s) -> result.add(s));
        lock.readLock().unlock();
        return result;
    }

    private void assertArgumentNotNull(Object argument, String name) {
        if (argument == null) {
            throw new IllegalArgumentException("Argument \"" + name + "\" is null");
        }
    }

    public List<Pair<String, Integer>> listTables() throws IOException {
        checkClosed();
        final List<Pair<String, Integer>> result = new ArrayList<>(tables.size());
        lock.readLock().lock();
        try {
            tables.forEach(new BiConsumer<String, StoreableTable>() {
                @Override
                public void accept(String s, StoreableTable table) {
                    result.add(new Pair<String, Integer>(s, table.size()));
                }
            });
        } catch (DatabaseException e) {
            throwIOException(e.getCause());
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    @Override
    public void close() throws Exception {
        checkClosed();
        final List<Exception> exceptions = new ArrayList<>();
        tables.forEach((s, table) -> {
            try {
                table.close();
            } catch (Exception e) {
                exceptions.add(e);
            }
        });
        closed.set(true);
        if (exceptions.size() > 0) {
            for (int i = 0; i + 1 < exceptions.size(); i++) {
                exceptions.get(i + 1).addSuppressed(exceptions.get(i));
            }
            throw exceptions.get(0);
        }
    }

    @Override
    public String toString() {
        checkClosed();
        return getClass().getSimpleName() + "[" + getRootDirectoryPath().toString() + "]";
    }
}
