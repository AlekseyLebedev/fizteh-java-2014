package ru.fizteh.fivt.students.LebedevAleksey.storeable.tests;

import org.junit.Assert;
import org.junit.Test;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.json.BrokenJsonException;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.json.JsonParser;
import ru.fizteh.fivt.students.LebedevAleksey.storeable.json.JsonUnsopportedObjectException;

import java.io.File;
import java.util.*;

public class JsonParserTest {
    @Test
    public void testGetJsonForStrings() throws JsonUnsopportedObjectException {
        Assert.assertEquals("\"Qwerty\"", JsonParser.getJson("Qwerty"));
        Assert.assertEquals("\"Test text\"", JsonParser.getJson("Test text"));
        Assert.assertEquals("\"Text, fore test. Asdf!\"", JsonParser.getJson("Text, fore test. Asdf!"));
    }

    @Test
    public void testGetJsonForStringsWithCaracters() throws JsonUnsopportedObjectException {
        Assert.assertEquals("\"Qwe\\nrty\"", JsonParser.getJson("Qwe\nrty"));
        Assert.assertEquals("\"Qwe\\re r\\ty\"", JsonParser.getJson("Qwe\re r\ty"));
        Assert.assertEquals("\"This is quote: \\\"test\\\".\"", JsonParser.getJson("This is quote: \"test\"."));
        Assert.assertEquals("\"\\b\"", JsonParser.getJson("\b"));
        Assert.assertEquals("\"\\f\"", JsonParser.getJson("\f"));
    }

    @Test(expected = JsonUnsopportedObjectException.class)
    public void testThrowExceptionForUnknownTypes() throws JsonUnsopportedObjectException {
        JsonParser.getJson(new File("."));
    }


    @Test()
    public void testCanSerialiseIntegers() throws JsonUnsopportedObjectException {
        Assert.assertEquals("1234", JsonParser.getJson(1234));
        Assert.assertEquals("123456789012345", JsonParser.getJson(123456789012345L));
        Assert.assertEquals("-4567", JsonParser.getJson(-4567));
        Assert.assertEquals("-923456789012345", JsonParser.getJson(-923456789012345L));
        Assert.assertEquals("123.45", JsonParser.getJson(123.45));
        Assert.assertEquals("123.45", JsonParser.getJson((float) 123.45));
        Assert.assertEquals("-123.45", JsonParser.getJson(-123.45));
        Assert.assertEquals("-123.45", JsonParser.getJson((float) -123.45));
        Assert.assertEquals("123.45", JsonParser.getJson((double) 123.45));
        Assert.assertEquals("-123.45", JsonParser.getJson((double) -123.45));
        Assert.assertEquals("123", JsonParser.getJson((byte) 123));
        Assert.assertEquals("-123", JsonParser.getJson((byte) -123));
    }

    @Test()
    public void testCanSerialiseBools() throws JsonUnsopportedObjectException {
        Assert.assertEquals("true", JsonParser.getJson(true));
        Assert.assertEquals("false", JsonParser.getJson(false));
    }

    @Test()
    public void testCanSerialiseNull() throws JsonUnsopportedObjectException {
        Assert.assertEquals("null", JsonParser.getJson(null));
    }

    @Test
    public void testCanGetJsonForArrays() throws JsonUnsopportedObjectException {
        Assert.assertEquals("[123,\"QwertY\"]", JsonParser.getJson(Arrays.asList(new Object[]{123, "QwertY"})));
        Assert.assertEquals("[123,\"QwertY\",true]", JsonParser.getJson(new Object[]{123, "QwertY", true}));
    }


    @Test
    public void testCanGetJsonForMap() throws JsonUnsopportedObjectException {
        checkMap(new TreeMap<>());
        checkMap(new HashMap<>());
    }

    private void checkMap(Map<String, Object> map) throws JsonUnsopportedObjectException {
        map.put("b", true);
        map.put("c", null);
        map.put("dd", 3.1415);
        String bString = "\"b\":true";
        String cString = "\"c\":null";
        String dString = "\"dd\":3.1415";
        String[] strings = new String[]{bString, cString, dString};
        int equalsCount = 0;
        equalsCount = assertMapJson(map, strings, equalsCount);
        strings[2] = cString;
        strings[1] = dString;
        equalsCount = assertMapJson(map, strings, equalsCount);
        strings[0] = cString;
        strings[2] = bString;
        equalsCount = assertMapJson(map, strings, equalsCount);
        strings[2] = dString;
        strings[1] = bString;
        equalsCount = assertMapJson(map, strings, equalsCount);
        strings[0] = dString;
        strings[2] = cString;
        equalsCount = assertMapJson(map, strings, equalsCount);
        strings[1] = cString;
        strings[2] = bString;
        equalsCount = assertMapJson(map, strings, equalsCount);
        Assert.assertEquals(1, equalsCount);
    }

    private int assertMapJson(Map<String, Object> map, String[] strings, int equalsCount)
            throws JsonUnsopportedObjectException {
        if (("{" + strings[0] + "," + strings[1] + "," + strings[2] + "}").equals(JsonParser.getJson(map))) {
            ++equalsCount;
        }
        return equalsCount;
    }

    @Test
    public void testSubItemJson() throws JsonUnsopportedObjectException {
        Map<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        Object[] array = new Object[]{1, map, new Object[]{1.2, new Object[]{true}, null}};
        Assert.assertEquals("[1,{\"a\":1},[1.2,[true],null]]", JsonParser.getJson(array));
    }

    @Test
    public void testCanJsonSupportedArrays() throws JsonUnsopportedObjectException {
        Integer[] array = new Integer[]{1, 2, 3};
        Assert.assertEquals("[1,2,3]", (String) JsonParser.getJson(array));
    }


    @Test
    public void testParseStringJson() throws BrokenJsonException {
        Assert.assertEquals("qwertY", JsonParser.parseJson("\"qwertY\""));
        Assert.assertEquals("qwe rtY", JsonParser.parseJson("\"qwe rtY\""));
        Assert.assertEquals("qwe\"rtY", JsonParser.parseJson("\"qwe\\\"rtY\""));
        Assert.assertEquals("\b\n\f\t\r\\/", JsonParser.parseJson("\"\\b\\n\\f\\t\\r\\\\\\/\""));
        Assert.assertEquals("Quick brown fox jump other the lazy dog.",
                JsonParser.parseJson("\"Quick brown fox jump other the lazy dog.\""));
        Assert.assertEquals("\u0077cd", JsonParser.parseJson("\"\\u0077cd\""));
        Assert.assertEquals("\u9abc", JsonParser.parseJson("\"\\u9abc\""));
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenStringJson1() throws BrokenJsonException {
        JsonParser.parseJson("\"\\u\"");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenStringJson2() throws BrokenJsonException {
        JsonParser.parseJson("\"\\u1\"");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenStringJson3() throws BrokenJsonException {
        JsonParser.parseJson("\"\\u12\"");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenStringJson4() throws BrokenJsonException {
        JsonParser.parseJson("\"\\u123\"");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenStringJson5() throws BrokenJsonException {
        JsonParser.parseJson("\"\\u12x3\"");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenStringJson6() throws BrokenJsonException {
        JsonParser.parseJson("\"aba");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenStringJson7() throws BrokenJsonException {
        JsonParser.parseJson("\"a\\hba\"");
    }

    @Test
    public void testIntParse() throws BrokenJsonException {
        JsonParser.setNeedShortIntegerTypes(false);
        Object value = JsonParser.parseJson("123");
        Assert.assertEquals(Long.class, value.getClass());
        Assert.assertEquals(123L, value);
        JsonParser.setNeedShortIntegerTypes(true);
        value = JsonParser.parseJson("123");
        Assert.assertEquals(Byte.class, value.getClass());
        Assert.assertEquals((byte) 123, value);
        value = JsonParser.parseJson("1234");
        Assert.assertEquals(Integer.class, value.getClass());
        Assert.assertEquals(1234, value);
        JsonParser.setNeedShortIntegerTypes(false);
        value = JsonParser.parseJson("4565678");
        Assert.assertEquals(4565678L, value);
        value = JsonParser.parseJson("-4565678");
        Assert.assertEquals(-4565678L, value);
        value = JsonParser.parseJson("-456.5678");
        Assert.assertEquals(-456.5678, value);
        value = JsonParser.parseJson("123.45");
        Assert.assertEquals(123.45, value);
        value = JsonParser.parseJson("1.23E5");
        Assert.assertEquals(1.23E5, value);
        value = JsonParser.parseJson("-1.23E5");
        Assert.assertEquals(-1.23E5, value);
        value = JsonParser.parseJson("1.23E-5");
        Assert.assertEquals(1.23E-5, value);
        value = JsonParser.parseJson("-1.23E-5");
        Assert.assertEquals(-1.23E-5, value);
        value = JsonParser.parseJson("1.23e5");
        Assert.assertEquals(1.23E5, value);
        value = JsonParser.parseJson("-1.23e5");
        Assert.assertEquals(-1.23E5, value);
        value = JsonParser.parseJson("1.23e-5");
        Assert.assertEquals(1.23E-5, value);
        value = JsonParser.parseJson("-1.23e-5");
        Assert.assertEquals(-1.23E-5, value);
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenDouble0() throws BrokenJsonException {
        JsonParser.parseJson("12.34.56");
    }


    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenDouble1() throws BrokenJsonException {
        JsonParser.parseJson("12.34E5e6");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenDouble2() throws BrokenJsonException {
        JsonParser.parseJson("-12.34-56");
    }

    @Test
    public void testParseNull() throws BrokenJsonException {
        Assert.assertEquals(null, JsonParser.parseJson("null"));
    }

    @Test
    public void testParseTrue() throws BrokenJsonException {
        Assert.assertEquals(true, JsonParser.parseJson("true"));
    }

    @Test
    public void testParseFalse() throws BrokenJsonException {
        Assert.assertEquals(false, JsonParser.parseJson("false"));
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseNullWithMistakes() throws BrokenJsonException {
        Assert.assertEquals(null, JsonParser.parseJson("nul"));
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseTrueWithMistakes() throws BrokenJsonException {
        Assert.assertEquals(true, JsonParser.parseJson("tre"));
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseFalseWithMistakes() throws BrokenJsonException {
        Assert.assertEquals(false, JsonParser.parseJson("fals"));
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseFalseWithMistakes2() throws BrokenJsonException {
        Assert.assertEquals(false, JsonParser.parseJson("fal"));
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseFalseWithMistakes3() throws BrokenJsonException {
        Assert.assertEquals(false, JsonParser.parseJson("falsq"));
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseNullWithMistakesAdditionalText() throws BrokenJsonException {
        Assert.assertEquals(null, JsonParser.parseJson("nuller"));
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseTrueWithMistakesAdditionalText() throws BrokenJsonException {
        Assert.assertEquals(true, JsonParser.parseJson("trueer"));
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseFalseWithMistakesAdditionalText() throws BrokenJsonException {
        Assert.assertEquals(false, JsonParser.parseJson("falseer"));
    }

    @Test()
    public void testCanParseArraysAndSpacesOk() throws BrokenJsonException {
        checkArray(JsonParser.parseJson("[123.45,567,null,true,\"qwerty\"]"));
        checkArray(JsonParser.parseJson("    [123.45, 567,   null,true,\"qwerty\"]"));
        checkArray(JsonParser.parseJson(" [ 123.45 , 567, null, true, \"qwerty\"  ]"));
        checkArray(JsonParser.parseJson("[   123.45   ,   567   ,   null   ,   true   ,   \"qwerty\"    ]   "));
    }

    private void checkArray(Object result) {
        List<Object> array = (List<Object>) result;
        Assert.assertEquals(5, array.size());
        Assert.assertEquals(123.45, array.get(0));
        Assert.assertEquals(567L, array.get(1));
        Assert.assertEquals(null, array.get(2));
        Assert.assertEquals(true, array.get(3));
        Assert.assertEquals("qwerty", array.get(4));
    }


    @Test()
    public void testCanParseMaps() throws BrokenJsonException {
        checkMap(JsonParser.parseJson(
                "{\"a\":1,\"b\":-2.3e2,\"c\":null,\"def\":true,\"false\":false,\"E\":\"Text \\\"with\\n...\"}"));
        checkMap(JsonParser.parseJson(
                "   {   \"a\":1,\"b\":-2.3e2   , \"c\":null,\"def\":true, \"false\":false, \"E\""
                        + ":\"Text \\\"with\\n...\"  }   "));
        checkMap(JsonParser.parseJson(
                "    {\"a\":1,\"b\":-2.3e2         ,\"c\":null,\"def\":true   ,   \"false\":false,\"E\":\"Te"
                        + "xt \\\"with\\n...\"      }"));
        checkMap(JsonParser.parseJson(
                "{   \"a\":1,\"b\":-2.3e2,\"c\":null,\"def\":true,\"false\":false,\"E\":\"Text \\\"with\\n...\"}    "));
    }

    private void checkMap(Object result) {
        Map<String, Object> map = (Map<String, Object>) result;
        Assert.assertEquals(6, map.size());
        Assert.assertEquals(1L, map.get("a"));
        Assert.assertEquals(-230.0, map.get("b"));
        Assert.assertEquals(null, map.get("c"));
        Assert.assertEquals(true, map.get("def"));
        Assert.assertEquals(false, map.get("false"));
        Assert.assertEquals("Text \"with\n...", map.get("E"));
    }

    @Test()
    public void testCanParseMultiObjects() throws BrokenJsonException {
        checkMultiArray(JsonParser.parseJson("[\"qw\",{\"a\":\"b\"},[null,12.3,{}],[]]"));
        checkMultiArray(JsonParser.parseJson("[\"qw\",{\"a\" : \"b\"},[null,12.3,{    } ],[    ]]"));
        checkMultiArray(JsonParser.parseJson("[\"qw\"   ,{\"a\"  :  \"b\"  } , [null,12.3,{}],  [  ]  ]  "));
        checkMultiArray(JsonParser.parseJson("[\"qw\",{\"a\":\"b\"},[null ,12.3,   {}  ],  []  ]"));
    }

    private void checkMultiArray(Object result) {
        List<Object> array = (List<Object>) result;
        Assert.assertEquals(4, array.size());
        Assert.assertEquals("qw", array.get(0));
        Map<String, Object> map = (Map<String, Object>) array.get(1);
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("b", map.get("a"));
        List<Object> subList = (List<Object>) array.get(2);
        Assert.assertEquals(3, subList.size());
        Assert.assertEquals(null, subList.get(0));
        Assert.assertEquals(12.3, subList.get(1));
        Assert.assertEquals(0, ((Map) subList.get(2)).size());
        Assert.assertEquals(0, ((List) array.get(3)).size());
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenJson0() throws BrokenJsonException {
        JsonParser.parseJson("[   ");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenJson1() throws BrokenJsonException {
        JsonParser.parseJson("[");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenJson2() throws BrokenJsonException {
        JsonParser.parseJson("]");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenJson3() throws BrokenJsonException {
        JsonParser.parseJson("}");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenJson4() throws BrokenJsonException {
        JsonParser.parseJson("[");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenJson5() throws BrokenJsonException {
        JsonParser.parseJson("{");
    }


    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenJson6() throws BrokenJsonException {
        JsonParser.parseJson("\"");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenJson7() throws BrokenJsonException {
        JsonParser.parseJson("");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenJson8() throws BrokenJsonException {
        JsonParser.parseJson(" ");
    }

    @Test(expected = BrokenJsonException.class)
    public void testParseBrokenJson9() throws BrokenJsonException {
        JsonParser.parseJson("nula");
    }

    @Test(expected = BrokenJsonException.class)
    public void testWrongMapKey() throws BrokenJsonException {
        JsonParser.parseJson("{1:1}");
    }

    @Test(expected = BrokenJsonException.class)
    public void testNotTerminatedString() throws BrokenJsonException {
        JsonParser.parseJson("\"qwerty");
    }


    @Test(expected = JsonUnsopportedObjectException.class)
    public void testWrongMapSerialise() throws JsonUnsopportedObjectException {
        Map<Integer, Integer> map = new TreeMap<>();
        map.put(1, 2);
        JsonParser.getJson(map);
    }

    @Test
    public void testBrokenJSONExceptionCanSaveOffset() {
        BrokenJsonException ex = new BrokenJsonException("Test", 7);
        Assert.assertEquals("Test", ex.getMessage());
        Assert.assertEquals(7, ex.getOffsetError());
        ex = new BrokenJsonException("Message", 42);
        Assert.assertEquals("Message", ex.getMessage());
        Assert.assertEquals(42, ex.getOffsetError());
    }

}
