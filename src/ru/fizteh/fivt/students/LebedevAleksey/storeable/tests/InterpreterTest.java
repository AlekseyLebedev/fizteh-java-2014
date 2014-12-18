package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.interpreter.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class InterpreterTest {
    public static final String TEST_COMMAND_NAME = "test";
    private ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
    private PrintStream testOut = new PrintStream(testOutput);
    private ByteArrayOutputStream testError = new ByteArrayOutputStream();
    private PrintStream testErr = new PrintStream(testError);

    private StreamsContainer createStreamContainer(InputStream input) {
        return new StreamsContainer(testOut, testErr, input);
    }

    @Test
    public void testInvokeCommands() {
        Interpreter interpreter = createInterpreterWithTestCommand(null);
        interpreter.run(new String[]{TEST_COMMAND_NAME});
        Assert.assertEquals("TestMessage" + System.lineSeparator(), testOutput.toString());
    }

    @Test
    public void testBatchModeWithOneCommand() {
        Interpreter interpreter = createInterpreterWithTestCommand(null);
        interpreter.run(new String[]{TEST_COMMAND_NAME + ";", TEST_COMMAND_NAME + ";", TEST_COMMAND_NAME});
        String expected = "TestMessage" + System.lineSeparator() + "TestMessage"
                + System.lineSeparator() + "TestMessage" + System.lineSeparator();
        Assert.assertEquals(expected, testOutput.toString());
    }

    @Test
    public void testInteractiveModeWithOneCommand() {
        List<Command> commands = new ArrayList<>();
        commands.add(new Command("exit", 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                streams.getOut().println("TestMessage");
                state.exit();
                return false;
            }
        });
        Interpreter interpreter = new Interpreter(commands, new InterpreterState(),
                createStreamContainer(new ByteArrayInputStream(("exit" + System.lineSeparator()).getBytes())));
        interpreter.run(new String[0]);
        String expected = "$ TestMessage" + System.lineSeparator();
        Assert.assertEquals(expected, testOutput.toString());
    }

    @Test
    public void testInteractiveModeWithSeveralCommands() {
        Interpreter interpreter = createInterpreterWithTestAndExitCommands(
                new ByteArrayInputStream((TEST_COMMAND_NAME + System.lineSeparator() + TEST_COMMAND_NAME
                        + System.lineSeparator() + "exit" + System.lineSeparator()).getBytes()));
        interpreter.run(new String[0]);
        String expected = "$ TestMessage" + System.lineSeparator() + "$ TestMessage" + System.lineSeparator()
                + "$ Terminated" + System.lineSeparator();
        Assert.assertEquals(expected, testOutput.toString());
    }

    @Test
    public void testBatchModeWithSeveralCommands() {
        Interpreter interpreter = createInterpreterWithTestAndExitCommands(null);
        interpreter.run(new String[]{TEST_COMMAND_NAME + ";", TEST_COMMAND_NAME});
        String expected = "TestMessage" + System.lineSeparator() + "TestMessage" + System.lineSeparator();
        Assert.assertEquals(expected, testOutput.toString());
    }

    @Test
    public void testExitCommandInBatchMode() {
        Interpreter interpreter = createInterpreterWithTestAndExitCommands(null);
        interpreter.run(new String[]{TEST_COMMAND_NAME + ";exit;", TEST_COMMAND_NAME});
        String expected = "TestMessage" + System.lineSeparator() + "Terminated" + System.lineSeparator();
        Assert.assertEquals(expected, testOutput.toString());
    }

    @Test
    public void testCorrectArgumentsParsingBatchMode() {
        List<Command> commands = new ArrayList<>();
        commands.add(new Command("print") {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                streams.getOut().print(arguments.length);
                for (String str : arguments) {
                    streams.getOut().print(" " + str);
                }
                return true;
            }
        });

        compareBatch(commands, new String[]{"print\t"}, "0");
        compareBatch(commands, new String[]{"print "}, "0");
        compareBatch(commands, new String[]{"\tprint"}, "0");
        compareBatch(commands, new String[]{" print"}, "0");
        compareBatch(commands, new String[]{"print\t\t"}, "0");
        compareBatch(commands, new String[]{"print "}, "0");
        compareBatch(commands, new String[]{"\t\tprint"}, "0");
        compareBatch(commands, new String[]{"  print"}, "0");
        compareBatch(commands, new String[]{"print\t "}, "0");
        compareBatch(commands, new String[]{"print \t"}, "0");
        compareBatch(commands, new String[]{"\t print"}, "0");
        compareBatch(commands, new String[]{" \tprint"}, "0");

        compareBatch(commands, new String[]{"print", "\t; print", "123"}, "01 123");
        compareBatch(commands, new String[]{"print", "\t; print", "123", "45\t;print ;print"}, "02 123 4500");
        compareBatch(commands, new String[]{"print", "\t; print", "123", "45;\tprint; print"}, "02 123 4500");
        compareBatch(commands, new String[]{"print", "\t; print", "123", "45\t;\tprint ; print"}, "02 123 4500");
        compareBatch(commands, new String[]{"print", "\t; print", "123", "45 ;\tprint ;\tprint"}, "02 123 4500");
        compareBatch(commands, new String[]{"print", "\t; print", "123", "45\t; print\t; print"}, "02 123 4500");

        compareBatch(commands, new String[]{"\tprint"}, "0");
        compareBatch(commands, new String[]{"\t\tprint"}, "0");
        compareBatch(commands, new String[]{" \tprint"}, "0");
        compareBatch(commands, new String[]{"print "}, "0");
        compareBatch(commands, new String[]{"print\t"}, "0");
        compareBatch(commands, new String[]{"print \t"}, "0");
        compareBatch(commands, new String[]{"print\t "}, "0");
        compareBatch(commands, new String[]{"print  "}, "0");
        compareBatch(commands, new String[]{"print\t\t"}, "0");
        compareBatch(commands, new String[]{"print "}, "0");
        compareBatch(commands, new String[]{"\tprint\t"}, "0");
        compareBatch(commands, new String[]{" print \t"}, "0");
        compareBatch(commands, new String[]{"\t\tprint\t "}, "0");
        compareBatch(commands, new String[]{"print  "}, "0");
        compareBatch(commands, new String[]{"print\t\t"}, "0");

        commands.add(new Command("secondCommand") {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                streams.getOut().println("Output");
                return true;
            }
        });
        compareBatch(commands, new String[]{"print", ";secondCommand;print"},
                "0Output" + System.lineSeparator() + "0");
    }

    @Test
    public void testCorrectArgumentsParsingInteractiveMode() {
        List<Command> commands = new ArrayList<>();
        commands.add(new Command("print") {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                streams.getOut().print(arguments.length);
                for (String str : arguments) {
                    streams.getOut().print(" " + str);
                }
                return true;
            }
        });
        commands.add(new Command("exit", 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                streams.getOut().println("Terminated");
                state.exit();
                return false;
            }
        });
        compareInteractive(commands, "print 345 abc \"de fg\"         \"hij\"\t \tklmn;print\t;print\t;exit",
                "$ 5 345 abc de fg hij klmn00Terminated" + System.lineSeparator());
        compareInteractive(commands, "                  print          \t\t\t\t\t  test          \t\t\t       "
                        + System.lineSeparator() + "exit", "$ 1 test$ Terminated" + System.lineSeparator()
        );
        compareInteractive(commands,
                "\t\t\t\t\t    print\t\"qwe rty\tyu\"   1 2 3  \t\t  test   j;\tprint\t;\tprint; print ;exit",
                "$ 6 qwe rty\tyu 1 2 3 test j000Terminated" + System.lineSeparator());
        compareInteractive(commands, "exit\t", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, "exit ", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, "\texit", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, " exit", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, "exit\t\t", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, "exit ", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, "\t\texit", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, "  exit", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, "exit\t ", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, "exit \t", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, "\t exit", "$ Terminated" + System.lineSeparator());
        compareInteractive(commands, " \texit", "$ Terminated" + System.lineSeparator());
    }

    private void compareInteractive(List<Command> commands, String input, String expected) {
        Interpreter interpreter;
        prepareStreams();
        interpreter = new Interpreter(commands, new InterpreterState(), createStreamContainer(
                new ByteArrayInputStream(input.getBytes())));
        interpreter.run(new String[0]);
        Assert.assertEquals(expected, testOutput.toString());
    }

    private void compareBatch(List<Command> commands, String[] input, String expected) {
        Interpreter interpreter;
        prepareStreams();
        interpreter = new Interpreter(commands, new InterpreterState(), createStreamContainer(null));
        interpreter.run(input);
        Assert.assertEquals(expected, testOutput.toString());
    }

    private Interpreter createInterpreterWithTestCommand(InputStream input) {
        List<Command> commands = new ArrayList<>();
        commands.add(new Command(TEST_COMMAND_NAME, 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams) throws ArgumentException, ParserException {
                streams.getOut().println("TestMessage");
                return true;
            }
        });
        return new Interpreter(commands, new InterpreterState(), createStreamContainer(input));
    }

    private Interpreter createInterpreterWithTestAndExitCommands(InputStream input) {
        List<Command> commands = new ArrayList<>();
        commands.add(new Command(TEST_COMMAND_NAME, 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                streams.getOut().println("TestMessage");
                return true;
            }
        });
        commands.add(new Command("exit", 0) {
            @Override
            protected boolean action(InterpreterState state, String[] arguments, StreamsContainer streams)
                    throws ArgumentException, ParserException {
                streams.getOut().println("Terminated");
                state.exit();
                return false;
            }
        });
        return new Interpreter(commands, new InterpreterState(), createStreamContainer(input));
    }

    @Before
    public void prepareStreams() {
        testOutput.reset();
        testError.reset();
    }
}