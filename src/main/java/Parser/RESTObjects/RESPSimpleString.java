package Parser.RESTObjects;

import Parser.DataType;

public class RESPSimpleString implements RESPObject {
    private final String value;

    public RESPSimpleString(String source) {
        this.value = source;
    }

    @Override
    public DataType getType() {
        return DataType.SIMPLE_STRING;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "+" + this.value + "\r\n";
    }
}
