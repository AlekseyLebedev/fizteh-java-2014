package ru.fizteh.fivt.students.LebedevAleksey.storeable;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;

public class Storeable implements ru.fizteh.fivt.storage.structured.Storeable {
    public static final String INCORRECT_TYPE_MESSAGE = "Value type isn't correct.";
    private Object[] data;
    private StoreableTable table;

    public Storeable(Object[] data, StoreableTable table) {
        this.data = data;
        this.table = table;
    }

    @Override
    public void setColumnAt(int columnIndex, Object value) throws ColumnFormatException, IndexOutOfBoundsException {
        if (table.getColumnType(columnIndex) == value.getClass()) {
            data[columnIndex] = value;
        } else {
            throw new ColumnFormatException(INCORRECT_TYPE_MESSAGE);
        }
    }

    @Override
    public Object getColumnAt(int columnIndex) throws IndexOutOfBoundsException {
        return data[columnIndex];
    }

    @Override
    public Integer getIntAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (Integer) assertColumnType(columnIndex, int.class);
    }

    private Object assertColumnType(int columnIndex, Class<?> type) {
        if (table.getColumnType(columnIndex) == type) {
            return data[columnIndex];
        } else {
            throw new ColumnFormatException("This column type is not " + type.toString());
        }
    }

    @Override
    public Long getLongAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (Long) assertColumnType(columnIndex, long.class);
    }

    @Override
    public Byte getByteAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (byte) assertColumnType(columnIndex, byte.class);
    }

    @Override
    public Float getFloatAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (float) assertColumnType(columnIndex, float.class);
    }

    @Override
    public Double getDoubleAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (double) assertColumnType(columnIndex, double.class);
    }

    @Override
    public Boolean getBooleanAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (boolean) assertColumnType(columnIndex, boolean.class);
    }

    @Override
    public String getStringAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (String) assertColumnType(columnIndex, String.class);
    }
}
