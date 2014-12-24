package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.DatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelTest {
    final AtomicInteger numberOfAction = new AtomicInteger(0);
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File dbPath;
    private TableProvider database;
    private Table table;
    private final ThreadLocal<List<Class<?>>> columnTypes = new ThreadLocal<List<Class<?>>>() {
        @Override
        protected List<Class<?>> initialValue() {
            return Arrays.asList(Integer.class);
        }
    };

    @Before
    public void setUp() throws IOException {
        dbPath = temporaryFolder.newFolder("db");
        database = new DatabaseFactory().create(dbPath.getAbsolutePath());
        table = database.createTable("table", columnTypes.get());
    }

    @Test
    public void checkParallelWork() throws IOException, InterruptedException {
        Storeable value = database.createFor(table);
        value.setColumnAt(0, 5);
        table.put("a", value);
        value.setColumnAt(0, 7);
        table.put("b", value);
        table.commit();
        Thread t1 = new Thread(() -> {
            try {
                table.put("c", database.createFor(table));
                Storeable a = table.get("a");
                a.setColumnAt(0, 2);
                table.put("a", a);
                numberOfAction.incrementAndGet();
                waitFor(3);
                Assert.assertEquals(3, table.size());
                Assert.assertEquals((Integer) 2, table.get("a").getIntAt(0));
                Assert.assertEquals((Integer) 7, table.get("b").getIntAt(0));
                Assert.assertEquals(null, table.get("c").getIntAt(0));
                numberOfAction.incrementAndGet();
                waitFor(5);
                table.commit();
                numberOfAction.incrementAndGet();
                waitFor(7);
                Assert.assertEquals(2, table.size());
                Assert.assertEquals((Integer) 2, table.get("a").getIntAt(0));
                Assert.assertEquals(null, table.get("c").getIntAt(0));
                Assert.assertNotNull(database.getTable("t2"));
                Assert.assertNull(database.createTable("t2", columnTypes.get()));
                database.removeTable("t2");
                numberOfAction.incrementAndGet();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                table.remove("b");
                numberOfAction.incrementAndGet();
                waitFor(3);
                Assert.assertEquals(1, table.size());
                Assert.assertEquals((Integer) 5, table.get("a").getIntAt(0));
                numberOfAction.incrementAndGet();
                waitFor(6);
                Assert.assertEquals(2, table.size());
                Assert.assertEquals((Integer) 2, table.get("a").getIntAt(0));
                Assert.assertEquals(null, table.get("c").getIntAt(0));
                table.commit();
                database.createTable("t2", columnTypes.get());
                numberOfAction.incrementAndGet();
                waitFor(8);
                Assert.assertNull(database.getTable("t2"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        t1.start();
        t2.start();
        waitFor(2);
        Assert.assertEquals(2, table.size());
        Assert.assertEquals((Integer) 5, table.get("a").getIntAt(0));
        Assert.assertEquals((Integer) 7, table.get("b").getIntAt(0));
        numberOfAction.incrementAndGet();
        t1.join();
        t2.join();
    }

    private void waitFor(int value) {
        try {
            while (numberOfAction.get() != value) {
                //System.out.println(numberOfAction.get() + " " + value);
                Thread.currentThread().sleep(5);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
