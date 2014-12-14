package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.DatabaseFileStructureException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.LoadOrSaveException;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.Database;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.DatabaseFactory;

import java.io.File;
import java.io.IOException;

public class DatabaseTest {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();
    private static File dbPath;
    private static TableProvider db;

    @BeforeClass
    public static void setUpClass() throws IOException {
        dbPath = folder.newFolder("db");
        db = new DatabaseFactory().create(dbPath.getAbsolutePath());
    }

    @Before
    public void setUp() throws Exception {

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
            void Test() throws IOException {
                fileFoundInRootDirectory(new File("Test"));
            }
        }.Test();
    }

    @Test
    public void testGetTable() throws Exception {

    }

    @Test
    public void testCreateTable() throws Exception {

    }

    @Test
    public void testGetRootDirectoryPath() throws Exception {

    }

    @Test
    public void testRemoveTable() throws Exception {

    }

    @Test
    public void testDeserialize() throws Exception {

    }

    @Test
    public void testSerialize() throws Exception {

    }

    @Test
    public void testCreateFor() throws Exception {

    }

    @Test
    public void testCreateFor1() throws Exception {

    }
}