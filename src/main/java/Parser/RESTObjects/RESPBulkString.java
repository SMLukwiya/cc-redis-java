package Parser.RESTObjects;

import Parser.DataType;

public class RESPBulkString implements RESPObject {
    private final String value;

    public RESPBulkString(String value) {
        this.value = value;
    }

    @Override
    public DataType getType() {
        return DataType.BULK_STRING;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "$" + this.value.length() + "\r\n" + this.value + "\r\n";
    }
}
