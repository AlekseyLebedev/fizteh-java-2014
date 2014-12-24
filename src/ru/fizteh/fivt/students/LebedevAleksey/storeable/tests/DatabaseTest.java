package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.DatabaseFileStructureException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.LoadOrSaveException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.Pair;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.Database;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.DatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

public class DatabaseTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File dbPath;
    private TableProvider database;

    @Before
    public void setUp() throws IOException {
        dbPath = folder.newFolder("db");
        database = new DatabaseFactory().create(dbPath.getAbsolutePath());
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testThrowIOException() throws Exception {
        try {
            Database.throwIOException(new LoadOrSaveException("Text1", new IOException("Text2")));
            Assert.fail("Have to throw exception");
        } catch (IOException e) {
            Assert.assertEquals("Text2", e.getMessage());
        }
        try {
            Database.throwIOException(new LoadOrSaveException("Text1", new IndexOutOfBoundsException("Text2")));
            Assert.fail("Have to throw exception");
        } catch (IOException e) {
            Assert.assertEquals("Text1", e.getMessage());
        }
        try {
            Database.throwIOException(new LoadOrSaveException("Text1"));
            Assert.fail("Have to throw exception");
        } catch (IOException e) {
            Assert.assertEquals("Text1", e.getMessage());
        }
        try {
            Database.throwIOException(new DatabaseFileStructureException("Text1", new IOException("Text2")));
            Assert.fail("Have to throw exception");
        } catch (IOException e) {
            Assert.assertEquals("Text2", e.getMessage());
        }
        try {
            Database.throwIOException(new DatabaseFileStructureException("Text1",
                    new IndexOutOfBoundsException("Text2")));
            Assert.fail("Have to throw exception");
        } catch (IOException e) {
            Assert.assertEquals("Text1", e.getMessage());
        }
        try {
            Database.throwIOException(new DatabaseFileStructureException("Text1"));
            Assert.fail("Have to throw exception");
        } catch (IOException e) {
            Assert.assertEquals("Text1", e.getMessage());
        }
    }

    @Test(expected = IOException.class)
    public void testFileFoundInRootDirectory() throws IOException {
        new Database(dbPath.getAbsolutePath()) {
            void test() throws IOException {
                fileFoundInRootDirectory(new File("Test"));
            }
        }.test();
    }

    @Test
    public void testGetAndCreateNonExistentTable() throws Exception {
        database.createTable("test", Arrays.asList(String.class));
        Assert.assertNotNull(database.getTable("test"));
        Assert.assertEquals("test", database.getTable("test").getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTableNullName() throws Exception {
        database.createTable(null, Arrays.asList(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTableNullTypes() throws Exception {
        database.createTable("abc", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTableNullInList() throws Exception {
        database.createTable("abc", Arrays.asList(Integer.class, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveNull() throws Exception {
        database.removeTable(null);
    }

    @Test
    public void testCreateExistingTable() throws Exception {
        database.createTable("table", Arrays.asList(String.class, Integer.class, Boolean.class));
        Assert.assertNull(database.createTable("table", Arrays.asList(String.class, Integer.class, Boolean.class)));
    }

    @Test
    public void testRemoveExistingTable() throws Exception {
        database.createTable("table", Arrays.asList(String.class, Integer.class, Boolean.class));
        database.removeTable("table");
        Assert.assertNull(database.getTable("table"));
        Assert.assertNotNull(database.createTable("table", Arrays.asList(String.class,
                Long.class, Double.class)));
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveNotExistentTable() throws Exception {
        database.removeTable("notExist");
    }

    @Test
    public void testSerializeDeserialize() throws Exception {
        Table table = database.createTable("table", Arrays.asList(String.class, Integer.class, Boolean.class,
                Long.class, Double.class, Float.class, Byte.class, Byte.class));
        List<Object> expected = Arrays.asList("qwerty", Integer.MIN_VALUE, true,
                23L, 1.23, -3.4E4f, (byte) 127, null);
        Storeable original = database.createFor(table, expected);
        String serialized = database.serialize(table, original);
        Storeable deserialized = database.deserialize(table, serialized);
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(expected.get(i), deserialized.getColumnAt(i));
        }
        table = database.createTable("name", Arrays.asList(Integer.class, String.class));
        deserialized = database.deserialize(table, "[null,null]");
        Assert.assertNull(deserialized.getColumnAt(0));
        Assert.assertNull(deserialized.getColumnAt(1));
    }

    @Test(expected = ParseException.class)
    public void testDeserializeWrongNumberOfColumns() throws ParseException, IOException {
        Table table = database.createTable("table", Arrays.asList(String.class, Integer.class));
        database.deserialize(table, "[null]");
    }

    @Test(expected = ParseException.class)
    public void testDeserializeWrongNumberOfColumns2() throws ParseException, IOException {
        Table table = database.createTable("table", Arrays.asList(String.class, Integer.class));
        database.deserialize(table, "[null,0,0]");
    }


    @Test
    public void testCreateFor() throws Exception {
        Table table = database.createTable("table", Arrays.asList(
                String.class, Integer.class, Boolean.class, Long.class, Double.class, Float.class, Byte.class));
        List<Object> values = Arrays.asList("qwerty", Integer.MAX_VALUE, true,
                -23L, -1.23, 3.4E4f, (byte) 127);
        Storeable storeable = database.createFor(table, values);
        for (int i = 0; i < values.size(); i++) {
            Assert.assertEquals(values.get(i), storeable.getColumnAt(i));
        }
    }

    @Test
    public void testCreateForEmptyAndStoreableSet() throws Exception {
        Table table = database.createTable("table", Arrays.asList(
                Integer.class, Long.class, String.class, Boolean.class, Double.class, Float.class, Byte.class));
        Storeable actual = database.createFor(table);
        try {
            actual.setColumnAt(0, 1);
            actual.setColumnAt(0, -1);
            actual.setColumnAt(1, -3L);
            actual.setColumnAt(2, "qwerty");
            actual.setColumnAt(3, false);
            actual.setColumnAt(3, true);
            actual.setColumnAt(4, 5.5);
            actual.setColumnAt(5, 2.3f);
            actual.setColumnAt(6, ((byte) 1));
            for (int i = 0; i < 7; i++) {
                actual.setColumnAt(i, null);
            }
            for (int i = 2; i < 7; i++) {
                try {
                    actual.setColumnAt(i, 1);
                    Assert.fail("Should be exception");
                } catch (ColumnFormatException e) {
                    // It's ok
                }
            }
        } catch (ColumnFormatException e) {
            Assert.fail("Error in setting column " + e);
        }
    }

    @Test
    public void testListTables() throws Exception {
        database.createTable("t1", Arrays.asList(String.class, Integer.class, Boolean.class));
        database.createTable("t2", Arrays.asList(Double.class, String.class, Long.class));
        List<Pair<String, Integer>> real = ((Database) database).listTables();
        Assert.assertEquals(2, real.size());
        Assert.assertEquals(0, (int) real.get(0).getValue());
        Assert.assertEquals(0, (int) real.get(1).getValue());
        String s1 = real.get(0).getKey();
        String s2 = real.get(1).getKey();
        Assert.assertTrue((s1.equals("t1") && s2.equals("t2")) || (s1.equals("t2") && s2.equals("t1")));
    }


    @Test
    public void testGetTableNames() throws Exception {
        database.createTable("t1", Arrays.asList(String.class, Integer.class, Boolean.class));
        database.createTable("t2", Arrays.asList(Double.class, String.class, Long.class));
        List<String> real = ((Database) database).getTableNames();
        Assert.assertEquals(2, real.size());
        String s1 = real.get(0);
        String s2 = real.get(1);
        Assert.assertTrue((s1.equals("t1") && s2.equals("t2")) || (s1.equals("t2") && s2.equals("t1")));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetTableThrowsExceptionOnClosedDatabase() throws Exception {
        Table table = database.createTable("name", Arrays.asList(Integer.class));
        ((Database) database).close();
        database.getTable("name");
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateTableThrowsExceptionOnClosedDatabase() throws Exception {
        ((Database) database).close();
        database.createTable("name", Arrays.asList(Integer.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveTableThrowsExceptionOnClosedDatabase() throws Exception {
        Table table = database.createTable("name", Arrays.asList(Integer.class));
        ((Database) database).close();
        database.removeTable("name");
    }

    @Test(expected = IllegalStateException.class)
    public void testDeserializeThrowsExceptionOnClosedDatabase() throws Exception {
        Table table = database.createTable("name", Arrays.asList(Integer.class));
        ((Database) database).close();
        database.deserialize(table, "[1]");
    }

    @Test(expected = IllegalStateException.class)
    public void testSerializeThrowsExceptionOnClosedDatabase() throws Exception {
        Table table = database.createTable("name", Arrays.asList(Integer.class));
        Storeable sto = database.createFor(table);
        ((Database) database).close();
        database.serialize(table, sto);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateForThrowsExceptionOnClosedDatabase() throws Exception {
        Table table = database.createTable("name", Arrays.asList(Integer.class));
        ((Database) database).close();
        database.createFor(table);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateFor1ThrowsExceptionOnClosedDatabase() throws Exception {
        Table table = database.createTable("name", Arrays.asList(Integer.class));
        ((Database) database).close();
        database.createFor(table, Arrays.asList(1));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetTableNamesThrowsExceptionOnClosedDatabase() throws Exception {
        ((Database) database).close();
        database.getTableNames();
    }


    @Test(expected = IllegalStateException.class)
    public void testToStringThrowsExceptionOnClosedDatabase() throws Exception {
        ((Database) database).close();
        database.toString();
    }

    @Test(expected = IllegalStateException.class)
    public void testListTablesThrowsExceptionOnClosedDatabase() throws Exception {
        ((Database) database).close();
        ((Database) database).listTables();
    }

    @Test(expected = IllegalStateException.class)
    public void testCloseThrowsExceptionOnClosedDatabase() throws Exception {
        ((Database) database).close();
        ((Database) database).close();
    }

    @Test(expected = IllegalStateException.class)
    public void testTableClosedAfterProviderClosed() throws Exception {
        Table table = database.createTable("name", Arrays.asList(Integer.class));
        ((Database) database).close();
        table.list();
    }

    @Test()
    public void testToString() throws Exception {
        Assert.assertEquals("Database[" + dbPath.getAbsolutePath() + "]", database.toString());

    }
}
