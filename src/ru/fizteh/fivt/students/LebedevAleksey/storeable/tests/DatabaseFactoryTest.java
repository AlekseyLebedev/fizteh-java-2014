package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.DatabaseFactory;

public class DatabaseFactoryTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testClose() throws Exception {
        DatabaseFactory factory = new DatabaseFactory();
        TableProvider database = factory.create(folder.newFolder("qwerty").getPath());
        factory.close();
        try {
            factory.create(folder.newFolder("qw").getPath());
            Assert.fail("Should throw exception");
        } catch (IllegalStateException e) {
            // Expected control flow
        }
        try {
            database.getTableNames();
            Assert.fail("Should throw exception");
        } catch (IllegalStateException e) {
            // Expected control flow
        }
    }
}