package ru.fizteh.fivt.students.LebedevAleksey.storeable;


import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.TableNotFoundException;
import ru.fizteh.fivt.students.LebedevAleksey.junit.DatabaseException;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter.*;

import java.io.IOException;
import java.text.ParseException;

public abstract class TableCommand extends Command {
    public TableCommand(String commandName, int argCount) {
        super(commandName, argCount);
    }

    private static Table getCurrentTable(InterpreterState state) throws TableNotFoundException {
        Table table = ((DatabaseState) state).getCurrentTable();
        if (table == null) {
            System.out.println("no table");
            throw new TableNotFoundException("No table selected");
        } else {
            return table;
        }
    }

    @Override
    protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
            throws ArgumentException, ParserException {
        try {
            mainAction(getCurrentTable(state), arguments, streams, ((DatabaseState) state).getDatabase());
            return true;
        } catch (DatabaseException | IOException e) {
            streams.getErr().println(e.getMessage());
            return false;
        } catch (TableNotFoundException e) {
            return false;
        } catch (ParseException e) {
            streams.getErr().println("wrong type (" + e.getMessage() + ")");
            return false;
        }
    }

    protected abstract void mainAction(Table table, String[] arguments, StreamsContainer streams, Database database)
            throws IOException, ParseException;
}
