package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.storage.structured.TableProviderFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseFactory implements TableProviderFactory, AutoCloseable {
    final AtomicBoolean closed = new AtomicBoolean(false);
    List<Database> databases = new ArrayList<>();

    @Override
    public synchronized TableProvider create(String path) throws IOException {
        checkClosed();
        Database database = new Database(path);
        databases.add(database);
        return database;
    }

    private void checkClosed() {
        if (closed.get()) {
            throw new IllegalStateException("TableProvider is closed");
        }
    }

    @Override
    public void close() throws Exception {
        checkClosed();
        final List<Exception> exceptions = new ArrayList<>();
        for (Database item : databases) {
            try {
                item.close();
            } catch (IllegalStateException e) {
                // Already closed, suppress
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        closed.set(true);
        if (exceptions.size() > 0) {
            for (int i = 0; i + 1 < exceptions.size(); i++) {
                exceptions.get(i + 1).addSuppressed(exceptions.get(i));
            }
            throw exceptions.get(0);
        }
    }
}
