package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Database implements TableProvider {
    public static final String TABLE_SIGNATURE_FILE_NAME = "signature.tsv";
    private static final String INCORRECT_NAME_OF_TABLES = "This name is not correct, folder can't be created";
    private static Map<String, Class<?>> stringTypesMap = new TreeMap<>();
    private static Map<Class<?>, String> typesStringMap = new TreeMap<>();

    static {
        stringTypesMap.put("int", int.class);
        stringTypesMap.put("long", long.class);
        stringTypesMap.put("byte", byte.class);
        stringTypesMap.put("float", float.class);
        stringTypesMap.put("double", double.class);
        stringTypesMap.put("boolean", boolean.class);
        stringTypesMap.put("String", String.class);
        typesStringMap.put(int.class, "int");
        typesStringMap.put(long.class, "long");
        typesStringMap.put(byte.class, "byte");
        typesStringMap.put(float.class, "float");
        typesStringMap.put(double.class, "double");
        typesStringMap.put(boolean.class, "boolean");
        typesStringMap.put(String.class, "String");

    }

    private final Path directoryPath;
    private Map<String, StoreableTable> tables = new TreeMap<>();


    public Database(String directory) {
        directoryPath = new File(directory).toPath();
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
                    try (FileOutputStream stream = new FileOutputStream(path.resolve(TABLE_SIGNATURE_FILE_NAME).toString())) {
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

    private Path getRootDirectoryPath() {
        return directoryPath;
    }

    private StoreableTable generateTable(String name) {
        return null;//TODO
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
        //TODO
        throw new NotImplementedException();
        return null;
    }

    @Override
    public String serialize(Table table, Storeable value) throws ColumnFormatException {
        //TODO
        throw new NotImplementedException();
        return null;
    }

    @Override
    public Storeable createFor(Table table) {
        //TODO
        throw new NotImplementedException();
        return null;
    }

    @Override
    public Storeable createFor(Table table, List<?> values) throws ColumnFormatException, IndexOutOfBoundsException {
        //TODO
        throw new NotImplementedException();
        return null;
    }
}
