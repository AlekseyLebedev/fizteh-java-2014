package ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter;

import java.util.*;

public class Interpreter {
    public static final String SLASH_SPECIAL_STRING = "#s";
    public static final String QUOTE_SPECIAL_STRING = "#q";
    public static final String SEMICOLON_SPECIAL_STRING = "#d";
    private boolean hasCorrectTerminated = false;

    private InterpreterState currentState;
    private StreamsContainer streams;
    private List<Command> commandsList;
    private int exitCode = 0;

    public Interpreter(List<Command> commands, InterpreterState interpreterState, StreamsContainer container) {
        commandsList = commands;
        currentState = interpreterState;
        streams = container;
    }

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
                    arguments.addAll(Arrays.asList(token.getValue().split("\\s")));
                }
            }
        }
        ArrayList<String> emptyArgs = new ArrayList<>();
        for (String arg : arguments) {
            if (arg.equals("")) {
                emptyArgs.add(arg);
            }
        }
        arguments.removeAll(emptyArgs);
        return arguments;
    }

    private static String trimOneEndSpace(String line) throws CannotParseCommandException {
        if (line.length() != 0) {
            char endChar = line.charAt(line.length() - 1);
            if (endChar == ' ' || endChar == '\t') {
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
            char startChar = line.charAt(0);
            if (startChar == ' ' || startChar == '\t') {
                return line.substring(1);
            } else {
                throw new CannotParseCommandException("Where is no space between to arguments.");
            }
        }
    }

    private List<ParsedCommand> parseCommand(String input) throws ParserException {
        input = replaceSpecialChars(input);
        List<CommandToken> tokensByQuote = splitCommandByQuote(input);
        ArrayList<ArrayList<CommandToken>> commandsTokens = splitCommands(tokensByQuote);
        return getParsedCommands(commandsTokens);
    }

    private String replaceSpecialChars(String input) {
        input = input.replace("#", "##");
        input = input.replace("\\\\", SLASH_SPECIAL_STRING);
        input = input.replace("\\\"", QUOTE_SPECIAL_STRING);
        input = input.replace("\\;", SEMICOLON_SPECIAL_STRING);
        return input;
    }

    private List<ParsedCommand> parseCommand(String[] input) throws ParserException {
        ArrayList<ArrayList<CommandToken>> commandsTokens = new ArrayList<>();
        commandsTokens.add(new ArrayList<>());
        for (String arg : input) {
            String[] tokens = replaceSpecialChars(arg).split(";", -1);
            addAtEnd(commandsTokens, tokens[0]);
            for (int i = 1; i < tokens.length; ++i) {
                commandsTokens.add(new ArrayList<>());
                addAtEnd(commandsTokens, tokens[i]);
            }
        }
        return getParsedCommands(commandsTokens);
    }

    private void addAtEnd(ArrayList<ArrayList<CommandToken>> commandsTokens, String arg) {
        commandsTokens.get(commandsTokens.size() - 1).add(new CommandToken(arg, false));
    }

    private ArrayList<ArrayList<CommandToken>> splitCommands(List<CommandToken> tokensByQuote) {
        ArrayList<ArrayList<CommandToken>> commandsTokens = new ArrayList<>(tokensByQuote.size());
        commandsTokens.add(new ArrayList<>());
        for (CommandToken token : tokensByQuote) {
            if (token.isWasInQuotes()) {
                commandsTokens.get(commandsTokens.size() - 1).add(token);
            } else {
                String[] tokens = token.getValue().split(";", -1);
                for (int j = 0; j < tokens.length; ++j) {
                    if (j > 0) {
                        commandsTokens.add(new ArrayList<>());
                    }
                    addAtEnd(commandsTokens, tokens[j]);
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
            result.setCommandName(((arguments.size() > 0) ? removeSpecialChars(arguments.get(0)) : null));
            String[] realArguments = new String[(arguments.size() > 0) ? arguments.size() - 1 : 0];
            for (int i = 1; i < arguments.size(); ++i) {
                realArguments[i - 1] = removeSpecialChars(arguments.get(i));
            }
            result.setArguments(realArguments);
            commands.add(result);
        }

        return commands;
    }

    private String removeSpecialChars(String text) {
        text = replaceSpecialString(text, SLASH_SPECIAL_STRING, "\\");
        text = replaceSpecialString(text, SEMICOLON_SPECIAL_STRING, ";");
        text = replaceSpecialString(text, QUOTE_SPECIAL_STRING, "\"");
        return text.replace("##", "#");
    }

    private String replaceSpecialString(String text, String mask, String maskValue) {
        int index = 0;
        do {
            index = text.indexOf(mask, index);
            if (index >= 0) {
                int j = index - 1;
                while (j >= 0 && text.charAt(j) == '#') {
                    j--;
                }
                if ((index - j) % 2 == 0) {
                    ++index;
                } else {
                    text = text.substring(0, index) + maskValue + text.substring(index + mask.length());
                }
            }
        }
        while (index >= 0);
        return text;
    }

    public final void run(String[] args) {
        if (args.length == 0) {
            Scanner scanner = new Scanner(streams.getIn());
            String command;
            do {
                try {
                    streams.getOut().print("$ ");
                    command = scanner.nextLine();
                    invokeCommand(command);
                } catch (NoSuchElementException ex) {
                    streams.getErr().println("Error: Can not read");
                    break;
                }
            } while (!isCorrectTerminated());
        } else {
            if (invokeCommand(args)) {
                hasCorrectTerminated = true;
            }
        }
        if (!isCorrectTerminated()) {
            exitCode = 1;
        }
    }

    public boolean invokeCommand(String input) {
        try {
            List<ParsedCommand> commands = parseCommand(input);
            return invokeCommands(commands);
        } catch (CommandInvokeException ex) {
            printInvokeError(ex);
        } catch (ParserException ex) {
            streams.getErr().println("Error: " + ex.getMessage());
        }
        streams.getErr().flush();
        return false;
    }

    protected void printInvokeError(CommandInvokeException ex) {
        streams.getErr().println(new StringBuilder().append(ex.getCommandName()).append(": ").append(ex.getMessage()));
    }

    public boolean invokeCommand(String[] input) {
        try {
            List<ParsedCommand> commands = parseCommand(input);
            return invokeCommands(commands);
        } catch (CommandInvokeException ex) {
            streams.getErr().println(new StringBuilder().append(ex.getCommandName()).append(": ") + ex.getMessage());
        } catch (ParserException ex) {
            streams.getErr().println("Error: " + ex.getMessage());
        }
        streams.getErr().flush();
        return false;
    }

    protected boolean invokeCommands(List<ParsedCommand> commands) throws ParserException {
        String lastCommand = "";
        for (ParsedCommand command : commands) {
            if (command.getCommandName() != null) {
                try {
                    boolean foundCommand = false;
                    lastCommand = command.getCommandName();
                    for (Command cmd : commandsList) {
                        if (cmd.getName().equals(command.getCommandName())) {
                            foundCommand = true;
                            if (!cmd.invoke(currentState, command, streams)) {
                                if (currentState.exited()) {
                                    return exit();
                                } else {
                                    return false;
                                }
                            }
                            break;
                        }
                    }
                    if (!foundCommand) {
                        throw new ParserException("This command is unknown: " + command.getCommandName());
                    }
                } catch (ArgumentException ex) {
                    streams.getErr().println(lastCommand + ": " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    protected final boolean exit() {
        this.hasCorrectTerminated = true;
        return false;
    }

    public boolean isCorrectTerminated() {
        return hasCorrectTerminated;
    }

    public int getExitCode() {
        return exitCode;
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
