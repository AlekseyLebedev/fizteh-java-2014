package ru.fizteh.fivt.students.LebedevAleksey.storeable.json;

import ru.fizteh.fivt.students.LebedevAleksey.MultiFileHashMap.Pair;

import java.util.*;

public abstract class JsonParser {
    public static final String UNEXPECTED_STOP_MESSAGE = "Parsers didn't reach the end.";
    private static final ThreadLocal<Boolean> NEED_SHORT_INTEGER_TYPES = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public static void setNeedShortIntegerTypes(boolean needShortIntegerTypes) {
        JsonParser.NEED_SHORT_INTEGER_TYPES.set(needShortIntegerTypes);
    }

    public static String getJson(Object data) throws JsonUnsupportedObjectException {
        StringBuilder result = new StringBuilder();
        createJson(data, result);
        return result.toString();
    }

    public static Object parseJson(String json) throws BrokenJsonException {
        json = json.trim();
        Pair<Object, Integer> result;
        try {
            result = parseJson(json, 0, null);
        } catch (JsonTerminatedException e) {
            throw new BrokenJsonException("Wrong JSON", json.length() > 0 ? json.length() - 1 : 0);
        }
        if (result.getValue() != json.length()) {
            throw new BrokenJsonException(UNEXPECTED_STOP_MESSAGE, result.getValue());
        }
        return result.getKey();
    }

    public static Pair<Object, Integer> parseJson(String json, int begin, Character possibleTerminator)
            throws BrokenJsonException, JsonTerminatedException {
        Character first = null;
        List<Object> list;
        Map<String, Object> map;
        int i;
        for (i = begin; i < json.length(); i++) {
            char symbol = json.charAt(i);
            if (possibleTerminator != null && possibleTerminator.equals(symbol)) {
                throw new JsonTerminatedException(i);
            }
            switch (symbol) {
                case ' ':
                    break;
                case '"':
                    return parseStringFromJson(json, i + 1);
                case '{':
                    return parseMapFromJson(json, i + 1);
                case '[':
                    return parseArrayFromJson(json, i + 1);
                case '-':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    return parseNumberFromJson(json, i);
                case 'n':
                case 't':
                case 'f':
                    try {
                        switch (json.substring(i, i + 4)) {
                            case "null":
                                return new Pair<>(null, i + 4);
                            case "true":
                                return new Pair<>(true, i + 4);
                            case "fals":
                                if ("false".equals(json.substring(i, i + 5))) {
                                    return new Pair<>(false, i + 5);
                                } else {
                                    throw new BrokenJsonException("Unexpected symbol in position " + i + 4, i + 4);
                                }
                            default:
                                throw new BrokenJsonException("Wrong token", i);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        throw new BrokenJsonException("Unexpected symbol \"" + symbol + "\" in position " + i, e, i);
                    }
                default:
                    throw new BrokenJsonException("Unexpected symbol \"" + symbol + "\" in position " + i, i);
            }
        }
        throw new BrokenJsonException(UNEXPECTED_STOP_MESSAGE, i);
    }

    private static Pair<Object, Integer> parseMapFromJson(String json, int begin) throws BrokenJsonException {
        Map<String, Object> map = new TreeMap<>();
        int i = begin;
        while (i < json.length()) {
            Pair<Object, Integer> result = null;
            try {
                result = parseJson(json, i, '}');
            } catch (JsonTerminatedException e) {
                return searchNextChar(json, map, i, '}');
            }
            String key;
            try {
                key = (String) result.getKey();
            } catch (ClassCastException e) {
                throw new BrokenJsonException("Wrong map key", i);
            }
            i = result.getValue();
            boolean exited = false;
            for (; i < json.length() && (!exited); ++i) {
                char symbol = json.charAt(i);
                switch (symbol) {
                    case ' ':
                        break;
                    case ':':
                        exited = true;
                        break;
                    default:
                        break;
                }
            }
            try {
                result = parseJson(json, i, null);
            } catch (JsonTerminatedException e) {
                throw new BrokenJsonException("JSON is not correct", e.getOffset());
            }
            map.put(key, result.getKey());
            i = result.getValue();
            exited = false;
            for (; i < json.length() && (!exited); ++i) {
                char symbol = json.charAt(i);
                switch (symbol) {
                    case ' ':
                        break;
                    case ',':
                        exited = true;
                        break;
                    case '}':
                        return new Pair<>(map, i + 1);
                    default:
                        break;
                }
            }
        }
        throw new BrokenJsonException(UNEXPECTED_STOP_MESSAGE, i);
    }

    private static Pair<Object, Integer> searchNextChar(String json, Object value, int i, char mask) {
        while (json.charAt(i) != mask) {
            ++i;
        }
        return new Pair<>(value, i + 1);
    }


    private static Pair<Object, Integer> parseArrayFromJson(String json, int begin) throws BrokenJsonException {
        List<Object> list = new ArrayList<>();
        int i = begin;
        while (i < json.length()) {
            Pair<Object, Integer> result = null;
            try {
                result = parseJson(json, i, ']');
            } catch (JsonTerminatedException e) {
                return searchNextChar(json, list, i, ']');
            }
            list.add(result.getKey());
            i = result.getValue();
            boolean exited = false;
            for (; i < json.length() && (!exited); ++i) {
                char symbol = json.charAt(i);
                switch (symbol) {
                    case ' ':
                        break;
                    case ',':
                        exited = true;
                        break;
                    case ']':
                        return new Pair<>(list, i + 1);
                    default:
                        break;
                }
            }
        }
        throw new BrokenJsonException(UNEXPECTED_STOP_MESSAGE, i);
    }

    private static Pair<Object, Integer> parseNumberFromJson(String json, int begin) throws BrokenJsonException {
        List<Character> posibleCharcters = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            posibleCharcters.add((char) ('0' + i));
        }
        posibleCharcters.add('+');
        posibleCharcters.add('-');
        posibleCharcters.add('E');
        posibleCharcters.add('e');
        posibleCharcters.add('.');
        int i;
        for (i = begin; i < json.length(); i++) {
            char symbol = json.charAt(i);
            if (!posibleCharcters.contains(symbol)) {
                break;
            }
        }
        String number = json.substring(begin, i);
        if (NEED_SHORT_INTEGER_TYPES.get()) {
            try {
                byte byteValue = Byte.parseByte(number);
                return new Pair<>(byteValue, i);
            } catch (NumberFormatException e) {
                // Next type...
            }
            try {
                int intValue = Integer.parseInt(number);
                return new Pair<>(intValue, i);
            } catch (NumberFormatException e) {
                // Next type...
            }
        }
        try {
            long longValue = Long.parseLong(number);
            return new Pair<>(longValue, i);
        } catch (NumberFormatException e) {
            // Next type...
        }
        try {
            double doubleValue = Double.parseDouble(number);
            return new Pair<>(doubleValue, i);
        } catch (NumberFormatException e) {
            // Next type...
        }
        throw new BrokenJsonException("Wrong number", i);
    }

    private static Pair<Object, Integer> parseStringFromJson(String json, int begin)
            throws BrokenJsonException {
        StringBuilder builder = new StringBuilder();
        for (int i = begin; i < json.length(); i++) {
            char symbol = json.charAt(i);
            if (symbol == '\\') {
                i = parseSlashSequense(json, builder, i);
            } else {
                if (symbol == '"') {
                    return new Pair<>(builder.toString(), i + 1);
                } else {
                    builder.append(symbol);
                }
            }
        }
        throw new BrokenJsonException(UNEXPECTED_STOP_MESSAGE, json.length() - 1);
    }

    private static int parseSlashSequense(String json, StringBuilder builder, int i) throws BrokenJsonException {
        if (i + 1 == json.length()) {
            throw new BrokenJsonException("String doesn't finish", i);
        }
        switch (json.charAt(i + 1)) {
            case '"':
                builder.append("\"");
                return i + 1;
            case '\\':
                builder.append("\\");
                return i + 1;
            case '/':
                builder.append("/");
                return i + 1;
            case 'b':
                builder.append("\b");
                return i + 1;
            case 'f':
                builder.append("\f");
                return i + 1;
            case 'n':
                builder.append("\n");
                return i + 1;
            case 'r':
                builder.append("\r");
                return i + 1;
            case 't':
                builder.append("\t");
                return i + 1;
            case 'u':
                if (i + 5 >= json.length()) {
                    throw new BrokenJsonException("Error in \\u in string", i);
                }
                try {
                    int num = Integer.parseInt(json.substring(i + 2, i + 6), 16);
                    builder.append((char) num);
                    return i + 5;
                } catch (NumberFormatException e) {
                    throw new BrokenJsonException("Error in \\u in string: can't parse int", e, i + 2);
                }
            default:
                throw new BrokenJsonException("Unknown \\ sequence in char number " + i, i);
        }
    }

    private static void createJson(Object data, StringBuilder builder) throws JsonUnsupportedObjectException {
        if (data == null) {
            builder.append("null");
            return;
        }
        if (containsInterface(data, Map.class)) {
            Map<String, ?> map;
            try {
                map = (Map<String, ?>) data;
            } catch (ClassCastException e) {
                throw new JsonUnsupportedObjectException("Only Map<String, ?> is supported, when you use map", e);
            }
            createJson(map, builder);
            return;
        }
        if (containsInterface(data, Iterable.class)) {
            createJson((Iterable) data, builder);
            return;
        }
        if (containsInterface(data, Object[].class)) {
            createJson((Object[]) data, builder);
            return;
        }
        if (data.getClass() == String.class) {
            createJson((String) data, builder);
            return;
        }
        if (data.getClass().getSuperclass() == Number.class) {
            builder.append(data.toString());
            return;
        }
        if (data.getClass() == Boolean.class) {
            builder.append(((boolean) data) ? "true" : false);
            return;
        }
        throw new JsonUnsupportedObjectException("This type (" + data.getClass() + ") is unsupported by JSON.");
    }

    private static void createJson(String data, StringBuilder builder) {
        data = data.replace("\\", "\\\\").replace("\"", "\\\"");
        data = data.replace("/", "\\/").replace("\b", "\\b");
        data = data.replace("\f", "\\f").replace("\n", "\\n");
        data = data.replace("\r", "\\r").replace("\t", "\\t");
        builder.append("\"");
        builder.append(data);
        builder.append("\"");
    }

    private static boolean containsInterface(Object data, Class<?> type) {
        try {
            type.cast(data);
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    private static void createJson(Iterable data, StringBuilder builder) throws JsonUnsupportedObjectException {
        builder.append("[");
        boolean first = true;
        for (Object item : data) {
            if (!first) {
                builder.append(",");
            } else {
                first = false;
            }
            createJson(item, builder);
        }
        builder.append("]");
    }

    private static void createJson(Object[] data, StringBuilder builder) throws JsonUnsupportedObjectException {
        builder.append("[");
        boolean first = true;
        for (Object item : data) {
            if (!first) {
                builder.append(",");
            } else {
                first = false;
            }
            createJson(item, builder);
        }
        builder.append("]");
    }

    private static void createJson(Map<String, ?> data, StringBuilder builder) throws JsonUnsupportedObjectException {
        try {
            builder.append("{");
            Set<String> keys = data.keySet();
            boolean notFirst = false;
            for (String item : keys) {
                if (notFirst) {
                    builder.append(",");
                } else {
                    notFirst = true;
                }
                createJson(item, builder);
                builder.append(":");
                createJson(data.get(item), builder);
            }
            builder.append("}");
        } catch (ClassCastException e) {
            throw new JsonUnsupportedObjectException("Only Map<String,?> is supported.", e);
        }
    }
}

class JsonTerminatedException extends Exception {
    private int offset;

    public JsonTerminatedException(int index) {
        super();
        offset = index;
    }

    public int getOffset() {
        return offset;
    }
}
