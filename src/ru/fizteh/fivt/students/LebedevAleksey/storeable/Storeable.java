package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Table;

import java.util.List;

public class Storeable implements ru.fizteh.fivt.storage.structured.Storeable {
    public static final String INCORRECT_TYPE_MESSAGE = "Value type isn't correct.";
    private List<Object> data;
    private Table table;

    public Storeable(List<Object> data, Table table) throws ColumnFormatException {
        if (data == null) {
            throw new IllegalArgumentException("Argument \"data\" is null");
        }
        if (table == null) {
            throw new IllegalArgumentException("Argument \"table\" is null");
        }
        if (data.size() != table.getColumnsCount()) {
            throw new IndexOutOfBoundsException("Argument arrays are different sizes");
        }
        for (int i = 0; i < data.size(); ++i) {
            Object item = data.get(i);
            if (item != null) {
                if (item.getClass() != table.getColumnType(i)) {
                    throw new ColumnFormatException("Wrong type of column number " + i);
                }
            }
        }
        this.data = data;
        this.table = table;
    }

    @Override
    public void setColumnAt(int columnIndex, Object value) throws ColumnFormatException, IndexOutOfBoundsException {
        if (value == null) {
            data.set(columnIndex, value);
        } else {
            if (table.getColumnType(columnIndex) == value.getClass()) {
                data.set(columnIndex, value);
            } else {
                throw new ColumnFormatException(INCORRECT_TYPE_MESSAGE);
            }
        }
    }

    @Override
    public Object getColumnAt(int columnIndex) throws IndexOutOfBoundsException {
        return data.get(columnIndex);
    }

    @Override
    public Integer getIntAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (Integer) assertColumnType(columnIndex, Integer.class);
    }

    private Object assertColumnType(int columnIndex, Class<?> type) {
        if (table.getColumnType(columnIndex) == type) {
            return data.get(columnIndex);
        } else {
            throw new ColumnFormatException("This column type is not " + type.toString());
        }
    }

    @Override
    public Long getLongAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (Long) assertColumnType(columnIndex, Long.class);
    }

    @Override
    public Byte getByteAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (byte) assertColumnType(columnIndex, Byte.class);
    }

    @Override
    public Float getFloatAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (float) assertColumnType(columnIndex, Float.class);
    }

    @Override
    public Double getDoubleAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (double) assertColumnType(columnIndex, Double.class);
    }

    @Override
    public Boolean getBooleanAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (boolean) assertColumnType(columnIndex, Boolean.class);
    }

    @Override
    public String getStringAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (String) assertColumnType(columnIndex, String.class);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[");
        boolean first = true;
        for (Object item : data) {
            if (first) {
                first = false;
            } else {
                builder.append(",");
            }
            if (item != null) {
                builder.append(item.toString());
            }
        }
        builder.append("]");
        return builder.toString();
    }
}