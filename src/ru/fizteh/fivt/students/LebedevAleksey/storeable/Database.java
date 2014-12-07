package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.json.BrokenJsonException;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.json.JsonParser;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.json.JsonUnsupportedObjectException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;

public class Database implements TableProvider {
    public static final String TABLE_SIGNATURE_FILE_NAME = "signature.tsv";
    private static final String INCORRECT_NAME_OF_TABLES = "This name is not correct, folder can't be created";
    private static Map<String, Class<?>> stringTypesMap = new TreeMap<>();
    private static Map<Class<?>, String> typesStringMap = new TreeMap<>();

    static {
        stringTypesMap.put("int", Integer.class);
        stringTypesMap.put("long", Long.class);
        stringTypesMap.put("byte", Byte.class);
        stringTypesMap.put("float", Float.class);
        stringTypesMap.put("double", Double.class);
        stringTypesMap.put("boolean", Boolean.class);
        stringTypesMap.put("String", String.class);
        typesStringMap.put(Integer.class, "int");
        typesStringMap.put(Long.class, "long");
        typesStringMap.put(Byte.class, "byte");
        typesStringMap.put(Float.class, "float");
        typesStringMap.put(java.lang.Double.class, "double");
        typesStringMap.put(Boolean.class, "boolean");
        typesStringMap.put(String.class, "String");

    }

    private Path directoryPath;
    private Map<String, StoreableTable> tables = new TreeMap<>();


    public Database(String directory) throws IOException {
        File root = new File(directory);
        directoryPath = root.toPath();
        File[] tables = root.listFiles();
        for (File file : tables) {
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
                    for (String item : tokens) {
                        Class<?> type = stringTypesMap.get(item);
                        if (type == null) {
                            throw new IOException("Wrong type name in signature of table " + tablename);
                        } else {
                            types.add(type);
                        }
                    }
                    StoreableTable table = new StoreableTable(tablename, this, types);
                    this.tables.put(tablename, table);
                } else {
                    throw new IOException("Where is not signature file in table " + tablename);
                }
            } else {
                fileFoundInRootDirectory(file);
            }
        }
    }

    protected void fileFoundInRootDirectory(File file) throws IOException {
        throw new IOException("There is file " + file.getName() + " in root directory");
    }

    private void assertNameNotNull(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Argument \"name\" is null");
        }
    }

    @Override
    public Table getTable(String name) {
        assertNameNotNull(name);
        return tables.get(name);
    }

    @Override
    public Table createTable(String name, List<Class<?>> columnTypes) throws IOException {
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
                StoreableTable table = generateTable(name);
                tables.put(name, table);
                return table;
            } else {
                throw new IllegalArgumentException(INCORRECT_NAME_OF_TABLES);
            }
        } else {
            return null;
        }
    }

    private String createTableSignature(List<Class<?>> columnTypes) {
        String tableSignature = "";
        for (Class<?> column : columnTypes) {
            if (column == null) {
                throw new IllegalArgumentException("Null column");
            }
            Class<?> type = stringTypesMap.get(column);
            if (type == null) {
                throw new IllegalArgumentException("Type is not supported");
            }
            tableSignature += type;
            tableSignature += " ";
        }
        tableSignature = tableSignature.trim();
        return tableSignature;
    }

    public Path getRootDirectoryPath() {
        return directoryPath;
    }

    private StoreableTable generateTable(String name) {
        return new StoreableTable(name, this, types);
    }

    @Override
    public void removeTable(String name) throws IOException {
        StoreableTable table = (StoreableTable) getTable(name);
        table.drop();
        tables.remove(table);
        Files.delete(directoryPath.resolve(name));
    }

    @Override
    public Storeable deserialize(Table table, String value) throws ParseException {
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
                    storable.setColumnAt(i, 0);
                } else if (data.get(i).getClass() == table.getColumnType(i)) {
                    storable.setColumnAt(i, data.get(i));
                } else if (!tryCastInteger(table, i, data.get(i), storable) &&
                        !tryCastFloat(table, i, data.get(i), storable)) {
                    throw new ParseException("Wrong data type in column number " + i, -1);
                }
            }
            return storable;
        } else {
            throw new ParseException("Wrong size of list: have " + data.size() + ", table have " +
                    table.getColumnsCount() + " columns.", value.length() - 1);
        }
    }

    private boolean tryCastInteger(Table table, int column, Object value, Storeable result) {
        if (table.getColumnType(column) == Integer.class || table.getColumnType(column) == Byte.class) {
            if (value.getClass() == Long.class) {
                if (table.getColumnType(column) == Integer.class) {
                    long val = (long) value;
                    if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                        result.setColumnAt(column, (int) value);
                        return true;
                    }
                } else {
                    if (table.getColumnType(column) == Byte.class) {
                        long val = (long) value;
                        if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
                            result.setColumnAt(column, (byte) val);
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
                    if (val >= Float.MIN_VALUE && val <= Float.MAX_VALUE) {
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
        return new ru.fizteh.fivt.students.LebedevAleksey.storeable.Storeable(Arrays.asList(
                new Object[table.getColumnsCount()]), table);
    }

    @Override
    public Storeable createFor(Table table, List<?> values) throws ColumnFormatException, IndexOutOfBoundsException {
        return new ru.fizteh.fivt.students.LebedevAleksey.storeable.Storeable((List<Object>) values, table);
    }
}
