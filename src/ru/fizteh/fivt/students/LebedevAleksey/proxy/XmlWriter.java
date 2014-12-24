package ru.fizteh.fivt.students.LebedevAleksey.proxy;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

public class XmlWriter {
    private Writer writer;
    private String initialNodeName;
    private Stack<String> lastNodes = new Stack<>();
    private boolean wasPlainText = false;

    public XmlWriter(Writer writer) {
        this.writer = writer;
    }

    public void writeBeginDocument(String initialNodeName) throws IOException, BadXmlException {
        this.initialNodeName = initialNodeName;
        write("<?xml version=\"1.0\"?>");
        writeBeginningTag(initialNodeName);
        writeEndingOfTag();
    }

    public void writeEndingOfTag() throws IOException, BadXmlException {
        write(">");
    }

    public void writeEndTag() throws IOException, BadXmlException {
        writeEndTag(1);
    }

    private void writeNewLine() throws IOException {
        writeln("");
        for (int i = 0; i < lastNodes.size(); i++) {
            write("    ");
        }
    }

    public void writePlainText(String text) throws IOException {
        wasPlainText = true;
        write(toXmlString(text));
    }

    private void writeEndTag(int minTagCount) throws IOException, BadXmlException {
        if (lastNodes.size() <= minTagCount) {
            throw new BadXmlException("There is no tags to close");
        } else {
            String tag = lastNodes.pop();
            if (!wasPlainText) {
                writeNewLine();
            }
            wasPlainText = false;
            write("</");
            write(tag);
            writeEndingOfTag();
        }
    }


    public void writeClosingEndingTag() throws IOException, BadXmlException {
        if (lastNodes.size() < 2) {
            throw new BadXmlException("There is no tags to close");
        } else {
            lastNodes.pop();
            wasPlainText = true;
            write("/");
            writeEndingOfTag();
        }
    }

    public void writeBeginningTag(String name) throws BadXmlException, IOException {
        checkSimpleText(name);
        writeNewLine();
        write("<");
        write(name);
        lastNodes.addElement(name);
    }

    public void writeBeginningTagSameLine(String name) throws BadXmlException, IOException {
        checkSimpleText(name);
        write("<");
        write(name);
        lastNodes.addElement(name);
    }

    private void checkSimpleText(String name) throws BadXmlException {
        if (!name.equals(toXmlString(name))) {
            throw new BadXmlException("Tag name is wrong");
        }
    }

    public void writeEndDocument() throws IOException, BadXmlException {
        if (initialNodeName == null) {
            writeEndTag(0);
        }
    }

    public void writeArgument(String name, String value) throws IOException, BadXmlException {
        checkSimpleText(name);
        write(" " + name + "=\"" + toXmlString(value) + "\"");
    }

    private void writeln(String text) throws IOException {
        write(text);
        write(System.lineSeparator());
    }

    private String toXmlString(String text) {
        return text.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&apos;").
                replace("<", "&lt;").replace(">", "&gt;").replace("\n", "&#10;").replace("\r", "$#13;");
    }

    private void write(String text) throws IOException {
        writer.append(text);
    }
}
