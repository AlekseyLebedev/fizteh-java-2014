package ru.fizteh.fivt.students.isalysultan.MultiFileHashMap;

import java.io.IOException;
import java.util.Scanner;

public class BatchMode {

    public static void batchParser(RootDirectory direct, String[] argv)
            throws IOException {
        @SuppressWarnings({ "unused", "resource" })
        Scanner in = new Scanner(System.in);
        StringBuilder allStringBuild = new StringBuilder();
        for (String argument : argv) {
            if (allStringBuild.length() != 0) {
                allStringBuild.append(' ');
            }
            allStringBuild.append(argument);
        }
        String allString = allStringBuild.toString();
        String[] commands = allString.split(";");
        int i = 0;
        CommandInterpretator newParser = new CommandInterpretator();
        boolean firstElement = true;
        while (i < commands.length) {
            if (firstElement) {
                String[] command = commands[0].split(" ");
                newParser.executeCommand(direct, command);
                firstElement = false;
            } else {
                String[] command = commands[i].trim().split(" ");
                newParser.executeCommand(direct, command);
            }
            ++i;
        }
        System.exit(0);
    }
}
