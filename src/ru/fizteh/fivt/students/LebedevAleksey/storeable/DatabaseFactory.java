package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.storage.structured.TableProviderFactory;

import java.io.IOException;

/**
 * Created by Алексей on 07.12.2014.
 */
public class DatabaseFactory implements TableProviderFactory {
    @Override
    public TableProvider create(String path) throws IOException {
        return new Database(path);
    }
}
