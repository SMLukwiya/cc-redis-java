package Parser;

import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPBulkString;
import Parser.RESTObjects.RESPObject;
import Parser.RESTObjects.RESPSimpleString;

import java.io.BufferedReader;
import java.io.IOException;

public class Parser {
    private final BufferedReader reader;

    public Parser(BufferedReader reader) {
        this.reader = reader;
    }

    public RESPObject parse() throws IOException {
        int prefix = reader.read();
        if (prefix == -1) {
            return null;
        }

        DataType type = DataType.getTypeFromPrefix((char) prefix);

        return switch (type) {
            case SIMPLE_STRING -> new RESPSimpleString(reader.readLine());
            case BULK_STRING -> parseBulkString();
            case ARRAY, PUSHES -> parseArray();
            default -> throw new IOException("Unknown RESP data type: " + (char) prefix);
        };
    }

    private RESPBulkString parseBulkString() throws IOException {
        int length = Integer.parseInt(reader.readLine());
        if (length == -1) {
            return null;
        }

        char[] chars = new char[length];
        reader.read(chars, 0, length);
        reader.readLine();
        return new RESPBulkString(new String(chars));
    }

    private RESPArray parseArray() throws IOException {
        int length = Integer.parseInt(reader.readLine());
        if (length == -1) {
            return null;
        }
        RESPObject[] items = new RESPObject[length];
        for (int i = 0; i < length; i++) {
            items[i] = parse();
            System.out.println();
        }
        return new RESPArray(items);
    }
}
