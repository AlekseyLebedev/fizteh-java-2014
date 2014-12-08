package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.strings.Table;
import ru.fizteh.fivt.storage.strings.TableProvider;
import ru.fizteh.fivt.students.LebedevAleksey.junit.AdditionalAssert;
import ru.fizteh.fivt.students.LebedevAleksey.junit.DatabaseException;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.Database;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.DatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class DatabaseTestWithWrapper {
    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();
    private static File dbPath;
    private static TableProvider database;

    @BeforeClass
    public static void setUpClass() throws IOException {
        dbPath = folder.newFolder("db");
        database = getTableProviderWrapper();
    }

    protected static TableProviderWrapper getTableProviderWrapper() throws IOException {
        return new TableProviderWrapper((Database) new DatabaseFactory().create(dbPath.getPath()));
    }

    @Test
    public void testGetAndCreateTable() {
        Assert.assertNull(database.getTable("abc"));
        Table table = database.createTable("abc");
        table.put("a", "a");
        table.commit();
        table = database.getTable("abc");
        Assert.assertEquals(1, table.size());
        AdditionalAssert.assertStringArrayContainsSameItemsAsList(new String[]{"a"}, table.list());
        Assert.assertEquals(null, database.getTable("abcd"));
        Assert.assertEquals(null, database.getTable("ab"));
        Assert.assertEquals(null, database.getTable("a"));
        Assert.assertEquals(null, database.getTable("qwerty"));
        database.removeTable("abc");
    }

    @Test(expected = IllegalStateException.class)
    public void testExceptionThrowsThanDropNotExistingTable() {
        database.removeTable("notExist");
    }

    @Test
    public void testCanRemoveTable() {
        Table table = database.createTable("name");
        table.put("a", "a");
        table.commit();
        database.removeTable("name");
        Assert.assertEquals(null, database.getTable("name"));
    }

    @Test
    public void testCanRemoveTableWithNoCommits() {
        Table table = database.createTable("name");
        database.removeTable("name");
        Assert.assertEquals(null, database.getTable("name"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTableThrowsExceptionThanArgumentIsNull() {
        database.createTable(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveTableThrowsExceptionThanArgumentIsNull() {
        database.removeTable(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetTableThrowsExceptionThanArgumentIsNull() {
        database.getTable(null);
    }

    @Test
    public void testReturnNullWhenTableExist() {
        database.createTable("qwerty");
        Assert.assertEquals(null, database.createTable("qwerty"));
        database.removeTable("qwerty");
    }

    @Test
    public void testGetTableFromManyTablesAndLoadDatabase() throws IOException {
        for (int i = 0; i < 10; i++) {
            database.createTable("t" + i);
        }
        for (int i = 0; i < 10; i++) {
            assertCanGetTableTNum(i);
        }
        database = getTableProviderWrapper();
        for (int i = 0; i < 10; i++) {
            database.removeTable("t" + i);
            for (int j = 0; j <= i; j++) {
                Assert.assertEquals(null, database.getTable("t" + j));
            }
            for (int j = i + 1; j < 10; j++) {
                assertCanGetTableTNum(j);
            }
        }
    }


    private void assertCanGetTableTNum(int i) {
        Assert.assertEquals("t" + i, database.getTable("t" + i).getName());
    }
}

class TableProviderWrapper implements ru.fizteh.fivt.storage.strings.TableProvider {
    private Database database;

    public TableProviderWrapper(Database tableProvider) {
        database = tableProvider;
    }

    @Override
    public Table getTable(String name) {
        return StringTableWrapper.create(database.getTable(name), database);
    }

    @Override
    public Table createTable(String name) {
        try {
            return StringTableWrapper.create(database.createTable(name, Arrays.asList(String.class)), database);
        } catch (IOException e) {
            throw new DatabaseException(e);
        }

    }

    @Override
    public void removeTable(String name) {
        try {
            database.removeTable(name);
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
    }
}

