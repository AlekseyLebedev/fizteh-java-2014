package ru.fizteh.fivt.students.LebedevAleksey.proxy;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.IdentityHashMap;

public class ProxyFactory implements ru.fizteh.fivt.proxy.LoggingProxyFactory {
    private long timestamp;
    private boolean testMode = false;

    public ProxyFactory() {
    }

    public ProxyFactory(int timestamp) {
        this.timestamp = timestamp;
        testMode = true;
    }

    public void finishLog(Writer writer) throws IOException {
        writer.append(System.lineSeparator() + "</log>");
    }

    @Override
    public Object wrap(Writer writer, Object implementation, Class<?> interfaceClass) {
        final XmlWriter xml = new XmlWriter(writer);
        try {
            xml.writeBeginDocument("log");
        } catch (Exception e) {
            // Eating this exception
        }
        InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (Arrays.asList(Object.class.getMethods()).contains(method)) {
                    if (method.getName().equals("toString")) {
                        return "Wrapper on " + implementation.toString();
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    if (method.getName().equals("hashCode")) {
                        return xml.hashCode();
                    }
                    if (method.getName().equals("clone")) {
                        return wrap(writer, implementation, interfaceClass);
                    }
                }
                try {
                    xml.writeBeginningTag("invoke");
                    xml.writeArgument("timestamp",
                            ((Long) (testMode ? timestamp : System.currentTimeMillis())).toString());
                    xml.writeArgument("class", implementation.getClass().getName());
                    xml.writeArgument("name", method.getName());
                    xml.writeEndingOfTag();
                    xml.writeBeginningTag("arguments");
                    if (args != null) {
                        xml.writeEndingOfTag();
                        for (int i = 0; i < args.length; i++) {
                            IdentityHashMap<Object, Object> map = new IdentityHashMap();
                            xml.writeBeginningTag("argument");
                            xml.writeEndingOfTag();
                            printValue(args[i], map);
                            xml.writeEndTag();
                        }
                        xml.writeEndTag();
                    } else {
                        xml.writeClosingEndingTag();
                    }
                } catch (Exception e) {
                    // Eating exception
                }
                try {
                    Object result = method.invoke(implementation, args);
                    if (method.getReturnType() != void.class) {
                        xml.writeBeginningTag("return");
                        xml.writeEndingOfTag();
                        IdentityHashMap<Object, Object> map = new IdentityHashMap();
                        printValue(result, map);
                        xml.writeEndTag();
                    }
                    return result;
                } catch (InvocationTargetException e) {
                    try {
                        xml.writeBeginningTag("thrown");
                        xml.writeEndingOfTag();
                        xml.writePlainText(e.getTargetException().toString());
                        xml.writeEndTag();
                    } catch (Exception ex) {
                        // Nothing to do with it
                    }
                    throw e.getCause();
                } catch (Exception e) {
                    return null;
                } finally {
                    try {
                        xml.writeEndTag();
                        xml.writeEndDocument();
                    } catch (Exception e) {
                        // Nothing to do with it
                    }
                }
            }

            private void printValue(Object item, IdentityHashMap<Object, Object> map)
                    throws IOException, BadXmlException {
                if (item == null) {
                    xml.writeBeginningTagSameLine("null");
                    xml.writeClosingEndingTag();
                    return;
                }
                if (map.containsKey(item)) {
                    xml.writeBeginningTagSameLine("cyclic");
                    xml.writeClosingEndingTag();
                    return;
                } else {
                    map.put(item, null);
                }
                if (item instanceof Iterable) {
                    xml.writeBeginningTag("list");
                    xml.writeEndingOfTag();
                    for (Object value : (Iterable) item) {
                        xml.writeBeginningTag("value");
                        xml.writeEndingOfTag();
                        printValue(value, map);
                        xml.writeEndTag();
                    }
                    xml.writeEndTag();
                    return;
                }
                xml.writePlainText(item.toString());
            }
        };
        return Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass}, invocationHandler);
    }
}
