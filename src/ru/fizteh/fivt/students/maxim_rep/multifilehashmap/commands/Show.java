package ru.fizteh.fivt.students.maxim_rep.multifilehashmap.commands;

import java.io.File;
import java.util.ArrayList;

import ru.fizteh.fivt.students.maxim_rep.multifilehashmap.DbMain;
import ru.fizteh.fivt.students.maxim_rep.multifilehashmap.TableDataMap;

public class Show implements DBCommand {

    public Show() {
    }

    private static String[] getTables() {
        ArrayList<String> tablesArray = new ArrayList<String>();

        if (!DbMain.databaseExists(DbMain.databasefilePath)) {
            return null;
        }

        File f = new File(DbMain.databasefilePath);
        for (File current : f.listFiles()) {
            if (current.isDirectory()) {
                try {
                    TableDataMap tempMap;
                    if (current.getName().equals(DbMain.currentTable)) {
                        tempMap = DbMain.fileStoredStringMap;
                    } else {
                        tempMap = new TableDataMap(DbMain.databasefilePath,
                                current.getName());
                    }
                    tablesArray.add(current.getName() + "\n" + tempMap.size());
                } catch (Exception e) {
                    System.err.println("Database Error");
                    return null;
                }

            }
        }

        String[] result = new String[tablesArray.size()];
        result = tablesArray.toArray(result);

        return result;

    }

    @Override
    public boolean execute() {
        String[] tableArray = getTables();
        if (tableArray == null) {
            return false;
        }
        for (String curLine : tableArray) {
            System.out.println(curLine.split("\n")[0] + " "
                    + curLine.split("\n")[1]);
        }
        return true;
    }

}
