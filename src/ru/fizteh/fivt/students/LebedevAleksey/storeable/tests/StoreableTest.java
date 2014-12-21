package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.DatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class StoreableTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File dbPath;
    private TableProvider database;
    private Table table;
    private Storeable storeable;

    @Before
    public void setUp() throws IOException {
        dbPath = folder.newFolder("db");
        database = new DatabaseFactory().create(dbPath.getAbsolutePath());
        table = database.createTable("table", Arrays.asList(
                Integer.class, Long.class, String.class, Boolean.class, Double.class, Float.class, Byte.class));
        storeable = database.createFor(table);
        storeable.setColumnAt(0, 1);
        storeable.setColumnAt(1, -3L);
        storeable.setColumnAt(2, "qwerty");
        storeable.setColumnAt(3, true);
        storeable.setColumnAt(4, 5.5);
        storeable.setColumnAt(5, 2.3f);
        storeable.setColumnAt(6, ((byte) 1));
    }

    @Test
    public void testCreateEmptyStoreable() {
        storeable = database.createFor(table);
        for (int i = 0; i < table.getColumnsCount(); i++) {
            Assert.assertNull(storeable.getColumnAt(i));
        }
    }

    @Test
    public void testGetIntAt() {
        Assert.assertEquals((Object) 1, storeable.getIntAt(0));
        for (int i = 0; i < table.getColumnsCount(); i++) {
            if (i != 0) {
                try {
                    storeable.getIntAt(i);
                    Assert.fail("Should be exception");
                } catch (ColumnFormatException e) {
                    // Ok.
                }
            }
        }
    }

    @Test
    public void testGetLongAt() throws Exception {
        Assert.assertEquals((Object) (-3L), storeable.getLongAt(1));
        for (int i = 0; i < table.getColumnsCount(); i++) {
            if (i != 1) {
                try {
                    storeable.getLongAt(i);
                    Assert.fail("Should be exception");
                } catch (ColumnFormatException e) {
                    // Ok.
                }
            }
        }
    }

    @Test
    public void testGetByteAt() throws Exception {
        Assert.assertEquals((Object) ((byte) 1), storeable.getByteAt(6));
        for (int i = 0; i < table.getColumnsCount(); i++) {
            if (i != 6) {
                try {
                    storeable.getByteAt(i);
                    Assert.fail("Should be exception");
                } catch (ColumnFormatException e) {
                    // Ok.
                }
            }
        }
    }

    @Test
    public void testGetFloatAt() throws Exception {
        Assert.assertEquals((Object) 2.3f, storeable.getFloatAt(5));
        for (int i = 0; i < table.getColumnsCount(); i++) {
            if (i != 5) {
                try {
                    storeable.getFloatAt(i);
                    Assert.fail("Should be exception");
                } catch (ColumnFormatException e) {
                    // Ok.
                }
            }
        }
    }

    @Test
    public void testGetDoubleAt() throws Exception {
        Assert.assertEquals((Object) 5.5, storeable.getDoubleAt(4));
        for (int i = 0; i < table.getColumnsCount(); i++) {
            if (i != 4) {
                try {
                    storeable.getDoubleAt(i);
                    Assert.fail("Should be exception");
                } catch (ColumnFormatException e) {
                    // Ok.
                }
            }
        }
    }

    @Test
    public void testGetBooleanAt() throws Exception {
        Assert.assertEquals((Object) true, storeable.getBooleanAt(3));
        for (int i = 0; i < table.getColumnsCount(); i++) {
            if (i != 3) {
                try {
                    storeable.getBooleanAt(i);
                    Assert.fail("Should be exception");
                } catch (ColumnFormatException e) {
                    // Ok.
                }
            }
        }
    }

    @Test
    public void testGetStringAt() throws Exception {
        Assert.assertEquals("qwerty", storeable.getStringAt(2));
        for (int i = 0; i < table.getColumnsCount(); i++) {
            if (i != 2) {
                try {
                    storeable.getStringAt(i);
                    Assert.fail("Should be exception");
                } catch (ColumnFormatException e) {
                    // Ok.
                }
            }
        }
    }
}
