package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.DatabaseFileStructureException;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.Pair;
import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.TableNotFoundException;
import ru.fizteh.fivt.students.LebedevAleksey.junit.DatabaseException;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        runInterpreter(args, getCommands());
    }

    public static void runInterpreter(String[] args, List<Command> commands) {
        try {
            DatabaseState state = new DatabaseState();
            Interpreter interpreter = new Interpreter(commands, state, new StreamsContainer());
            interpreter.run(args);
        } catch (DatabaseFileStructureException | IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(3);
        }
    }

    public static List<Command> getCommands() {
        List<Command> commands = Arrays.asList(new Command[]{new Command("exit", 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                StoreableTable currentTable = toDatabaseState(state).getCurrentTable();
                int changesCount = 0;
                if (currentTable != null) {
                    changesCount = currentTable.changesCount();
                }
                if (changesCount == 0) {
                    state.exit();
                    return false;
                } else {
                    streams.getOut().println(changesCount + " unsaved changes");
                }
                return true;
            }
        }, new Command("show", 1) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                if (arguments[0].equals("tables")) {
                    List<Pair<String, Integer>> tables;
                    try {
                        tables = toDatabaseState(state).getDatabase().listTables();
                    } catch (IOException e) {
                        streams.getErr().println(e.getMessage());
                        return false;
                    }
                    for (Pair<String, Integer> table : tables) {
                        streams.getOut().println(table.getKey() + " " + table.getValue());
                    }
                    return true;
                } else {
                    throw new ArgumentException("Unknown argument in show command");
                }
            }
        }, new Command("create") {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                if (arguments.length >= 2) {
                    if (arguments[1].startsWith("(") && arguments[arguments.length - 1].endsWith(")")) {
                        String name = arguments[0];
                        try {
                            String[] types = Arrays.copyOfRange(arguments, 1, arguments.length);
                            types[0] = types[0].substring(1);
                            String lastType = types[types.length - 1];
                            types[types.length - 1] = lastType.substring(0, lastType.length() - 1);
                            if (types.length == 1 && types[0].equals("")) {
                                types = new String[0];
                            }
                            if (toDatabaseState(state).getDatabase().
                                    createTable(name, Database.ParseTypes(types)) == null) {
                                streams.getOut().println(name + " exists");
                                return true;
                            }
                            streams.getOut().println("created");
                            return true;
                        } catch (IOException | DatabaseException e) {
                            streams.getErr().println(e.getMessage());
                            return false;
                        } catch (ParseException e) {
                            streams.getErr().println("wrong type (" + e.getMessage() + ")");
                            return false;
                        }
                    } else {
                        throw new ArgumentException("There is no correct information about columns");
                    }
                } else {
                    throw new ArgumentException("Not enough arguments");
                }
            }
        }, new Command("drop", 1) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                String name = arguments[0];
                try {
                    DatabaseState dbState = toDatabaseState(state);
                    if (dbState.getCurrentTable() != null && dbState.getCurrentTable().getName().equals(name)) {
                        dbState.setCurrentTable(null);
                    }
                    dbState.getDatabase().removeTable(name);
                    streams.getOut().println("dropped");
                } catch (IllegalStateException ex) {
                    streams.getOut().println(name + " not exists");
                    return false;
                } catch (IOException e) {
                    streams.getErr().println(e.getMessage());
                    return false;
                }
                return true;
            }
        }, new Command("use", 1) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                StoreableTable currentTable;
                int changesCount = 0;
                currentTable = toDatabaseState(state).getCurrentTable();
                if (currentTable != null) {
                    changesCount = currentTable.changesCount();
                }
                if (changesCount == 0) {
                    String name = arguments[0];
                    Table table = toDatabaseState(state).getDatabase().getTable(name);
                    if (table == null) {
                        streams.getOut().println(name + " not exists");
                    } else {
                        streams.getOut().println("using " + name);
                        toDatabaseState(state).setCurrentTable((StoreableTable) table);
                    }
                } else {
                    streams.getOut().println(changesCount + " unsaved changes");
                }
                return true;
            }
        }});
        List<Command> array = new ArrayList<>();
        array.addAll(commands);
        array.addAll(getTableCommands());
        return array;
    }

    public static Table getCurrentTable(InterpreterState state) throws TableNotFoundException {
        Table table = toDatabaseState(state).getCurrentTable();
        if (table == null) {
            System.out.println("no table");
            throw new TableNotFoundException("No table selected");
        } else {
            return table;
        }
    }

    public static DatabaseState toDatabaseState(InterpreterState state) {
        return (DatabaseState) state;
    }

    private static List<Command> getTableCommands() {
        return Arrays.asList(new Command("list", 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                try {
                    List<String> result;
                    result = ((StoreableTable) getCurrentTable(state)).list();
                    for (int i = 0; i < result.size(); i++) {
                        if (i > 0) {
                            streams.getOut().print(", ");
                        }
                        streams.getOut().print(result.get(i));
                    }
                    streams.getOut().println();
                    return true;
                } catch (IOException | DatabaseException e) {
                    streams.getErr().println(e.getMessage());
                    return false;
                } catch (TableNotFoundException e) {
                    return true;
                }
            }
        }, new Command("put", 2) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                try {
                    Database database = toDatabaseState(state).getDatabase();
                    Table table = getCurrentTable(state);
                    Storeable result = table.put(arguments[0], database.deserialize(table, arguments[1]));
                    if (result == null) {
                        streams.getOut().println("new");
                    } else {
                        streams.getOut().println("overwrite");
                        streams.getOut().println(database.serialize(table, result));
                    }
                    return true;
                } catch (TableNotFoundException e) {
                    return true;
                } catch (DatabaseException e) {
                    streams.getErr().println(e.getMessage());
                    return false;
                } catch (ParseException e) {
                    streams.getErr().println("wrong type (" + e.getMessage() + ")");
                    return false;
                }

            }
        }, new Command("get", 1) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                try {
                    Database database = toDatabaseState(state).getDatabase();
                    Table table = getCurrentTable(state);
                    Storeable result = table.get(arguments[0]);
                    if (result == null) {
                        streams.getOut().println("not found");
                    } else {
                        streams.getOut().println("found");
                        streams.getOut().println(database.serialize(table, result));
                    }
                    return true;
                } catch (DatabaseException e) {
                    System.err.println(e.getMessage());
                    return false;
                } catch (TableNotFoundException e) {
                    return true;
                }
            }
        }, new Command("remove", 1) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                try {
                    Table table = getCurrentTable(state);
                    if (table.remove(arguments[0]) != null) {
                        streams.getOut().println("removed");
                    } else {
                        streams.getOut().println("not found");
                    }
                    return true;
                } catch (TableNotFoundException e) {
                    return true;
                } catch (DatabaseException e) {
                    streams.getErr().println(e.getMessage());
                    return false;
                }
            }
        }, new Command("commit", 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                try {
                    streams.getOut().println(getCurrentTable(state).commit());
                    return true;
                } catch (IOException | DatabaseException e) {
                    streams.getErr().println(e.getMessage());
                    return false;
                } catch (TableNotFoundException e) {
                    return true;
                }
            }
        }, new Command("rollback", 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                try {
                    streams.getOut().println(getCurrentTable(state).rollback());
                    return true;
                } catch (DatabaseException e) {
                    streams.getErr().println(e.getMessage());
                    return false;
                } catch (TableNotFoundException e) {
                    return true;
                }
            }
        }, new Command("size", 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                try {
                    streams.getOut().println(getCurrentTable(state).size());
                    return true;
                } catch (DatabaseException e) {
                    streams.getErr().println(e.getMessage());
                    return false;
                } catch (TableNotFoundException e) {
                    return false;
                }
            }
        });
    }
}
