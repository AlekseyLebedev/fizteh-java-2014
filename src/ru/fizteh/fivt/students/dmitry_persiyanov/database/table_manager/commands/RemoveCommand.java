package ru.fizteh.fivt.students.dmitry_persiyanov.database.table_manager.commands;

import ru.fizteh.fivt.students.dmitry_persiyanov.database.exceptions.TableIsNotChosenException;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.table_manager.TableManager;

import java.io.PrintStream;

public class RemoveCommand extends TableManagerCommand {
    public RemoveCommand(final String[] args, final TableManager relatedTable) {
        super("remove", 1, args, relatedTable);
    }

    @Override
    protected void execute(final PrintStream out) throws TableIsNotChosenException {
        if (relatedTable == null) {
            throw new TableIsNotChosenException();
        } else {
            String key = args[0];
            String value = relatedTable.remove(key);
            if (value == null) {
                out.println("not found");
            } else {
                out.println("removed");
            }
        }
    }
}
