package ru.fizteh.fivt.students.LebedevAleksey.proxy.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.LebedevAleksey.proxy.ProxyFactory;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.DatabaseFactory;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ProxyFactoryTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File dbPath;
    private TableProvider database;

    @Before
    public void setUp() throws IOException {
        dbPath = folder.newFolder("db");
        database = new DatabaseFactory().create(dbPath.getAbsolutePath());
    }

    @Test
    public void testWrapOnTableProvider() throws Exception {
        ProxyFactory factory = new ProxyFactory(12345);
        CharArrayWriter writer = new CharArrayWriter();
        TableProvider provider = (TableProvider) (factory.wrap(writer, database, TableProvider.class));
        Table table = provider.createTable("name", Arrays.asList(Integer.class, String.class));
        Assert.assertNotNull(table);
        Assert.assertEquals(table, provider.getTable("name"));
        String path = dbPath.toPath().resolve("name").toString();
        Assert.assertEquals(("<?xml version=\"1.0\"?>\n" +
                "<log>\n" +
                "    <invoke timestamp=\"12345\" class=\"ru.fizteh.fivt.students.LebedevAleksey.storeable." +
                "Database\" name=\"createTable\">\n" +
                "        <arguments>\n" +
                "            <argument>name</argument>\n" +
                "            <argument>\n" +
                "                <list>\n" +
                "                    <value>class java.lang.Integer</value>\n" +
                "                    <value>class java.lang.String</value>\n" +
                "                </list>\n" +
                "            </argument>\n" +
                "        </arguments>\n" +
                "        <return>StoreableTable[" + path + "]</return>\n" +
                "    </invoke>\n" +
                "    <invoke timestamp=\"12345\" class=\"ru.fizteh.fivt.students.LebedevAleksey.storeable.Database\" " +
                "name=\"getTable\">\n" +
                "        <arguments>\n" +
                "            <argument>name</argument>\n" +
                "        </arguments>\n" +
                "        <return>StoreableTable[" + path + "]</return>\n" +
                "    </invoke>").replace("\n", System.lineSeparator()), new String(writer.toCharArray()));
        try {
            provider.getTable(null);
        } catch (IllegalArgumentException e) {
            // Ok
        }
        Assert.assertEquals(("<?xml version=\"1.0\"?>\n" +
                "<log>\n" +
                "    <invoke timestamp=\"12345\" class=\"ru.fizteh.fivt.students.LebedevAleksey.storeable." +
                "Database\" name=\"createTable\">\n" +
                "        <arguments>\n" +
                "            <argument>name</argument>\n" +
                "            <argument>\n" +
                "                <list>\n" +
                "                    <value>class java.lang.Integer</value>\n" +
                "                    <value>class java.lang.String</value>\n" +
                "                </list>\n" +
                "            </argument>\n" +
                "        </arguments>\n" +
                "        <return>StoreableTable[" + path + "]</return>\n" +
                "    </invoke>\n" +
                "    <invoke timestamp=\"12345\" class=\"ru.fizteh.fivt.students.LebedevAleksey.storeable.Database\" " +
                "name=\"getTable\">\n" +
                "        <arguments>\n" +
                "            <argument>name</argument>\n" +
                "        </arguments>\n" +
                "        <return>StoreableTable[" + path + "]</return>\n" +
                "    </invoke>\n" +
                "    <invoke timestamp=\"12345\" class=\"ru.fizteh.fivt.students.LebedevAleksey.storeable.Database" +
                "\" name=\"getTable\">\n" +
                "        <arguments>\n" +
                "            <argument><null/></argument>\n" +
                "        </arguments>\n" +
                "        <thrown>java.lang.IllegalArgumentException: Argument &quot;name&quot; is null</thrown>\n" +
                "    </invoke>").replace("\n", System.lineSeparator()), new String(writer.toCharArray()));
    }

    @Test
    public void testWrapOnTable() throws Exception {
        ProxyFactory factory = new ProxyFactory(12345);
        CharArrayWriter writer = new CharArrayWriter();
        Table newTable = database.createTable("name",
                Arrays.asList(Integer.class, String.class));
        newTable.put("a", database.createFor(newTable));
        newTable.commit();
        Table table = (Table) new ProxyFactory(12345).wrap(writer, newTable, Table.class);
        Assert.assertNotNull(table);
        Assert.assertEquals(1, table.size());
        Assert.assertEquals(("<?xml version=\"1.0\"?>\n" +
                "<log>\n" +
                "    <invoke timestamp=\"12345\" class=\"ru.fizteh.fivt.students.LebedevAleksey.storeable" +
                ".StoreableTable\" name=\"size\">\n" +
                "        <arguments/>\n" +
                "        <return>1</return>\n" +
                "    </invoke>").replace("\n", System.lineSeparator()), new String(writer.toCharArray()));
    }
}