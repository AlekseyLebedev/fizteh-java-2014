package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;

import org.junit.Assert;
import org.junit.Test;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter.*;

public class CommandTest {

    public static final int TEST_COUNT = 10;
    public static final String TEST_COMMAND_NAME = "testname";
    private InterpreterState state = new InterpreterState();
    private StreamsContainer streams = new StreamsContainer();

    @Test
    public void testInvoke() throws ParserException {
        Command command = new Command(TEST_COMMAND_NAME, 2) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams) {
                return arguments[0].equals(arguments[1]);
            }
        };
        String[] args = new String[]{"1", "1"};
        ParsedCommand information = new ParsedCommand();
        information.setCommandName(TEST_COMMAND_NAME);
        information.setArguments(args);
        Assert.assertTrue(command.invoke(state, information, streams));
        args[1] = "2";
        Assert.assertFalse(command.invoke(state, information, streams));
    }

    @Test(expected = ArgumentException.class)
    public void testWrongArgumentCountException() throws ParserException {
        Command cmd = new Command(TEST_COMMAND_NAME, 1) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                throw new ParserException("Only for test");
            }
        };
        ParsedCommand command = new ParsedCommand();
        command.setCommandName(TEST_COMMAND_NAME);
        command.setArguments(new String[0]);
        cmd.invoke(state, command, streams);
    }

    @Test
    public void testAutoCheckArguments() {
        ParsedCommand[] arguments = new ParsedCommand[TEST_COUNT];
        int[] results = new int[TEST_COUNT];
        for (int i = 0; i < TEST_COUNT; i++) {
            arguments[i] = new ParsedCommand();
            arguments[i].setCommandName(TEST_COMMAND_NAME);
            String[] args = new String[i];
            int sum = 0;
            for (int j = 0; j < i; j++) {
                args[j] = j + "";
                sum += j;
            }
            results[i] = sum;
            arguments[i].setArguments(args);
        }
        for (int i = 0; i < TEST_COUNT; i++) {
            final int[] result = {0};
            Command cmd = new Command(TEST_COMMAND_NAME, i) {
                @Override
                protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                        throws ArgumentException, ParserException {
                    int sum = 0;
                    for (String str : arguments) {
                        sum += Integer.parseInt(str);
                    }
                    result[0] = sum;
                    return true;
                }
            };
            for (int j = 0; j < TEST_COUNT; j++) {
                result[0] = 0;
                try {
                    Assert.assertTrue(cmd.invoke(state, arguments[j], streams));
                    Assert.assertEquals(result[0], results[j]);
                } catch (ParserException e) {
                    if (j == i) {
                        Assert.fail("Don't work with arguments");
                    }
                }
            }
        }
    }

    @Test
    public void testManualCheckArguments() throws ParserException {
        testManualArgsCheckingWork(new Command(TEST_COMMAND_NAME) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                if (arguments.length == 2) {
                    return true;
                } else {
                    throw new ArgumentException("My message");
                }
            }
        });
        testManualArgsCheckingWork(new Command(TEST_COMMAND_NAME, -1) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                if (arguments.length == 2) {
                    return true;
                } else {
                    throw new ArgumentException("My message");
                }
            }
        });
    }

    private void testManualArgsCheckingWork(Command cmd) throws ParserException {
        Assert.assertEquals(TEST_COMMAND_NAME, cmd.getName());
        ParsedCommand parsedCommand = new ParsedCommand();
        parsedCommand.setCommandName(TEST_COMMAND_NAME);
        parsedCommand.setArguments(new String[]{"", ""});
        Assert.assertTrue(cmd.invoke(state, parsedCommand, streams));
        try {
            parsedCommand.setArguments(new String[0]);
            cmd.invoke(state, parsedCommand, streams);
            Assert.fail("Manual check arguments don't work");
        } catch (ArgumentException e) {
            Assert.assertEquals("My message", e.getMessage());
        }
    }

    @Test
    public void testGetName() throws Exception {
        Command cmd = new Command(TEST_COMMAND_NAME, 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                return false;
            }
        };
        Assert.assertEquals(TEST_COMMAND_NAME, cmd.getName());
        cmd = new Command("someText", 1) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                return false;
            }
        };
        Assert.assertEquals("someText", cmd.getName());
    }

    @Test(expected = ParserException.class)
    public void testCanThrowParserException() throws ParserException {
        Command cmd = new Command(TEST_COMMAND_NAME, 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                throw new ParserException("Only for test");
            }
        };
        ParsedCommand command = new ParsedCommand();
        command.setCommandName(TEST_COMMAND_NAME);
        command.setArguments(new String[0]);
        cmd.invoke(state, command, streams);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckCommandName() throws ParserException {
        Command cmd = new Command(TEST_COMMAND_NAME, 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                return true;
            }
        };
        ParsedCommand parsedCommand = new ParsedCommand();
        parsedCommand.setCommandName("WrongName");
        parsedCommand.setArguments(new String[0]);
        cmd.invoke(state, parsedCommand, streams);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckCommandNameSecondCtor() throws ParserException {
        Command cmd = new Command(TEST_COMMAND_NAME) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                return true;
            }
        };
        ParsedCommand parsedCommand = new ParsedCommand();
        parsedCommand.setCommandName("WrongName");
        parsedCommand.setArguments(new String[0]);
        cmd.invoke(state, parsedCommand, streams);
    }

    @Test
    public void testStateExit() throws ParserException {
        InterpreterState myState = new InterpreterState();
        Assert.assertFalse(myState.exited());
        Command cmd = new Command(TEST_COMMAND_NAME, 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                state.exit();
                return false;
            }
        };
        ParsedCommand parsedCommand = new ParsedCommand();
        parsedCommand.setCommandName(TEST_COMMAND_NAME);
        parsedCommand.setArguments(new String[0]);
        Assert.assertFalse(cmd.invoke(myState, parsedCommand, streams));
        Assert.assertTrue(myState.exited());
    }

}
