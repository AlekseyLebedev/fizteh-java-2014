package ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter;

public abstract class Command {
    private String name;
    private int argumentsCount;

    public Command(String commandName, int argCount) {
        name = commandName;
        argumentsCount = argCount;
    }

    public Command(String commandName) {
        name = commandName;
        argumentsCount = -1;
    }

    public boolean invoke(InterpreterState interpreterState, ParsedCommand command, StreamsContainer streams)
            throws ArgumentException, ParserException {
        if (!command.getCommandName().equals(name)) {
            throw new IllegalArgumentException("Wrong interpreter command: " + command.getCommandName()
                    + ", but expected " + name);
        }
        if (argumentsCount >= 0 && command.getArguments().length != argumentsCount) {
            throw new ArgumentException("Invalid number of arguments: " + argumentsCount
                    + " expected, " + command.getArguments().length + " found");
        } else {
            return action(interpreterState, command.getArguments(), streams);
        }
    }

    protected abstract boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
            throws ArgumentException, ParserException;

    public String getName() {
        return name;
    }
}
