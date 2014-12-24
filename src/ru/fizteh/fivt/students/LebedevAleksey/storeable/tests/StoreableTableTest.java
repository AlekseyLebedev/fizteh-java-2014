package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.storage.structured.TableProviderFactory;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.DatabaseFactory;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.StoreableTable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class StoreableTableTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private Table table;
    private TableProvider provider;
    private TableProviderFactory factory;
    private Class<?>[] types = {Integer.class, String.class, Boolean.class};
    private File tempFolder;

    public StoreableTableTest() {
        this.factory = new DatabaseFactory();
    }

    @Before
    public void setUp() throws Exception {
        tempFolder = folder.newFolder();
        provider = factory.create(tempFolder.getAbsolutePath());
        table = provider.createTable("table", Arrays.asList(types));
    }

    @Test
    public void testPutGet() throws Exception {
        List<Object> expected = Arrays.asList(0, "qwerty", false);
        table.put("line", provider.createFor(table, expected));
        Storeable actual = table.get("line");
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.getColumnAt(i));
        }
    }

    @Test
    public void testRemove() throws Exception {
        List<Object> expected = Arrays.asList(1, "test", true);
        table.put("qwerty", provider.createFor(table, expected));
        Storeable actual = table.remove("qwerty");
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.getColumnAt(i));
        }
    }

    @Test
    public void testSize() throws Exception {
        List<Object> expected = Arrays.asList(3, "q", false);
        table.put("a", provider.createFor(table, expected));
        table.put("b", provider.createFor(table, expected));
        assertEquals(2, table.size());
    }

    @Test
    public void testGetColumnsCount() throws Exception {
        assertEquals(3, table.getColumnsCount());
    }

    @Test
    public void testGetColumnType() throws Exception {
        for (int i = 0; i < types.length; i++) {
            assertEquals(types[i], table.getColumnType(i));
        }
    }

    @Test
    public void testGetName() throws Exception {
        assertEquals("table", table.getName());
    }

    @Test
    public void testToString() {
        Assert.assertEquals("StoreableTable[" + tempFolder.toPath().resolve("table") + "]", table.toString());
    }

    @Test
    public void testRollabackAfterClosing() throws Exception {
        table.put("q", provider.createFor(table));
        table.commit();
        table.put("r", provider.createFor(table));
        ((StoreableTable) table).close();
        table = provider.getTable("table");
        Assert.assertEquals(1, table.size());
        Assert.assertNotNull(table.get("q"));
    }

    @Test
    public void testAllMethodsThrowsExceptionAfterClose() throws Exception {
        StoreableTable myTable = (StoreableTable) table;
        myTable.close();
        Method[] declaredMethods = myTable.getClass().getDeclaredMethods();
        for (Method item : declaredMethods) {
            if (Modifier.isPublic(item.getModifiers())) {
                Object[] args = new Object[item.getParameterTypes().length];
                Storeable storeable = provider.createFor(provider.getTable("table"));
                for (int i = 0; i < args.length; i++) {
                    if (item.getParameterTypes()[i] == String.class) {
                        args[i] = "qwerty";
                    } else {
                        if (item.getParameterTypes()[i] == Storeable.class) {
                            args[i] = storeable;
                        } else {
                            args[i] = 0;
                        }
                    }
                }
                try {
                    item.invoke(myTable, args);
                    Assert.fail("Should be exception invoking method " + item.getName());
                } catch (InvocationTargetException e) {
                    Assert.assertEquals(IllegalStateException.class, e.getTargetException().getClass());
                }
            }
        }
    }
}
