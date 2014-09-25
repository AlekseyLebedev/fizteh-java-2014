package ru.fizteh.fivt.students.LebedevAleksey.Shell01;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public abstract class CommandParser {
    private boolean hasCorrectTerminated = false;

    private static ArrayList<String> splitArguments(ArrayList<CommandToken> currentCommand)
            throws CannotParseCommandException {
        ArrayList<String> arguments = new ArrayList<>();
        for (int i = 0; i < currentCommand.size(); ++i) {
            CommandToken token = currentCommand.get(i);
            if (token.isWasInQuotes()) {
                arguments.add(token.getValue());
                if (i + 1 < currentCommand.size() && (!currentCommand.get(i + 1).isWasInQuotes())) {
                    currentCommand.get(i + 1).setValue(trimOneStartSpace(currentCommand.get(i + 1).getValue()));
                }
            } else {
                if (i + 1 < currentCommand.size() && currentCommand.get(i + 1).isWasInQuotes()) {
                    currentCommand.get(i).setValue(trimOneEndSpace(currentCommand.get(i).getValue()));
                }
                if (token.getValue().length() > 0) {
                    arguments.addAll(Arrays.asList(token.getValue().split(" ", -1)));
                }
            }
        }
        while (arguments.size() > 0 && (arguments.get(arguments.size() - 1).equals(""))) {
            arguments.remove(arguments.size() - 1);
        }
        int trimStartIndex = 0;
        while (trimStartIndex < arguments.size() && (arguments.get(trimStartIndex).equals(""))) {
            ++trimStartIndex;
        }
        for (int i = trimStartIndex; i < arguments.size(); ++i) {
            arguments.set(i - trimStartIndex, arguments.get(i));
        }
        for (int i = 0; i < trimStartIndex; ++i) {
            arguments.remove(arguments.size() - 1);
        }
        return arguments;
    }

    private static String trimOneEndSpace(String line) throws CannotParseCommandException {
        if (line.length() != 0) {
            if (line.charAt(line.length() - 1) == ' ') {
                return line.substring(0, line.length() - 1);
            } else {
                throw new CannotParseCommandException("Where is no space between to arguments.");
            }
        } else {
            return line;
        }
    }

    private static String trimOneStartSpace(String line) throws CannotParseCommandException {
        if (line.length() == 0) {
            return line;
        } else {
            if (line.charAt(0) == ' ') {
                return line.substring(1);
            } else {
                throw new CannotParseCommandException("Where is no space between to arguments.");
            }
        }
    }

    private List<ParsedCommand> parseCommand(String input) throws ParserException {
        List<CommandToken> tokensByQuote = splitCommandByQuote(input);
        ArrayList<ArrayList<CommandToken>> commandsTokens = splitCommands(tokensByQuote);
        return getParsedCommands(commandsTokens);
    }

    private ArrayList<ArrayList<CommandToken>> splitCommands(List<CommandToken> tokensByQuote) {
        ArrayList<ArrayList<CommandToken>> commandsTokens = new ArrayList<>(tokensByQuote.size());
        commandsTokens.add(new ArrayList<CommandToken>());
        for (CommandToken token : tokensByQuote) {
            if (token.isWasInQuotes()) {
                commandsTokens.get(commandsTokens.size() - 1).add(token);
            } else {
                String[] tokens = token.getValue().split("; ", -1);
                for (int j = 0; j < tokens.length; ++j) {
                    if (j > 0) {
                        commandsTokens.add(new ArrayList<CommandToken>());
                    }
                    commandsTokens.get(commandsTokens.size() - 1).add(new CommandToken(tokens[j], false));
                }
            }
        }
        return commandsTokens;
    }

    private List<CommandToken> splitCommandByQuote(String input) throws CannotParseCommandException {
        List<CommandToken> result = new ArrayList<>();
        int startIndex = 0;
        while (startIndex >= 0) {
            int index = input.indexOf('"', startIndex);
            if (index < 0) {
                result.add(new CommandToken(input.substring(startIndex), false));
            } else {
                result.add(new CommandToken(input.substring(startIndex, index), false));
                startIndex = index + 1;
                index = input.indexOf('"', startIndex);
                if (index < 0) {
                    throw new CannotParseCommandException("Wrong quote structure.");
                } else {
                    result.add(new CommandToken(input.substring(startIndex, index), true));
                }
            }
            startIndex = index;
            if (startIndex > 0) {
                ++startIndex;
            }
        }
        return result;
    }

    private List<ParsedCommand> getParsedCommands(ArrayList<ArrayList<CommandToken>> commandsTokens)
            throws CannotParseCommandException {
        List<ParsedCommand> commands = new ArrayList<>(commandsTokens.size());
        for (ArrayList<CommandToken> currentCommand : commandsTokens) {
            ArrayList<String> arguments = splitArguments(currentCommand);
            ParsedCommand result = new ParsedCommand();
            result.setCommandName(((arguments.size() > 0) ? arguments.get(0) : null));
            String[] realArguments = new String[(arguments.size() > 0) ? arguments.size() - 1 : 0];
            for (int i = 1; i < arguments.size(); ++i) {
                realArguments[i - 1] = arguments.get(i);
            }
            result.setArguments(realArguments);
            commands.add(result);
        }

        //TODO Remove
        logParsedResults(commands);

        return commands;
    }

    private void logParsedResults(List<ParsedCommand> commands) {
        Log.println("--Command Parse Output--");
        for (int i = 0; i < commands.size(); i++) {
            ParsedCommand command = commands.get(i);
            Log.print(command.getCommandName() + " | ");
            for (String str : command.getArguments()) {
                Log.print("'" + str + "' ");
            }
            Log.println();
        }
    }

    public final void run(String[] args) {
        if (args.length == 0) {
            Scanner scanner = new Scanner(System.in);
            String command;
            do {
                System.out.print("$ ");
                command = scanner.nextLine();
                invokeCommand(command);
            } while (!isCorrectTerminated());
        }
        if (!isCorrectTerminated()) {
            System.exit(1);
        }
    }

    public boolean invokeCommand(String input) {
        try {
            List<ParsedCommand> commands = parseCommand(input);
            return invokeCommands(commands);
        } catch (CommandInvokeException ex) {
            System.err.println(new StringBuilder().append(ex.getCommandName()).append(": ") + ex.getMessage());
        } catch (ParserException ex) {
            System.err.println("Error: " + ex.getMessage());
        }
        System.err.flush();
        return false;
    }

    protected abstract boolean invokeCommands(List<ParsedCommand> commands) throws ParserException;

    protected final boolean exit() {
        this.hasCorrectTerminated = true;
        return false;
    }

    public boolean isCorrectTerminated() {
        return hasCorrectTerminated;
    }

    private static class CommandToken {
        private String value;
        private boolean wasInQuotes;

        CommandToken(String value, boolean wasInQuotes) {
            this.value = value;
            this.wasInQuotes = wasInQuotes;
        }

        String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        boolean isWasInQuotes() {
            return wasInQuotes;
        }
    }
}


class ParsedCommand {
    private String[] arguments;
    private String commandName;

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String[] getArguments() {
        return arguments;
    }

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }
}

class Log {
    private static PrintStream writer;
    private static OutputStream stream;

    static {
        try {
            stream = new FileOutputStream("log.txt");
            try {
                writer = new PrintStream(stream, true);
            } catch (Throwable ex) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Throwable e) {
                        ex.addSuppressed(e);
                    }
                }
                throw ex;
            }
        } catch (Throwable ex) {
            System.err.println("Log error: " + ex.getMessage());
            closeStream();
        }
    }

    static void closeStream() {
        if (stream != null) {
            try {
                stream.close();
            } catch (Throwable ex) {
                System.err.println("Error closing log file: " + ex.getMessage());
            }
        }
    }


    static void println() {
        println("");
    }

    static void println(String line) {
        writer.println(line);
        writer.flush();
    }

    static void print(String line) {
        writer.print(line);
        writer.flush();
    }
}


class CannotParseCommandException extends ParserException {
    CannotParseCommandException(String message) {
        super(message);
    }
}

class CommandInvokeException extends ParserException {
    private final String commandName;

    CommandInvokeException(String message, String commandName) {
        super(message);
        this.commandName = commandName;
    }

    CommandInvokeException(String message, String commandName, Throwable ex) {
        super(message, ex);
        this.commandName = commandName;
    }

    public String getCommandName() {
        return commandName;
    }
}

class ParserException extends Exception {
    ParserException(String message) {
        super(message);
    }

    ParserException(String message, Throwable ex) {
        super(message, ex);
    }
}