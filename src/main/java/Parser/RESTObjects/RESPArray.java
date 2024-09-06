package Parser.RESTObjects;

import Parser.DataType;

public class RESPArray implements RESPObject {
    private final RESPObject[] values;

    public RESPArray(RESPObject[] values) {
        this.values = values;
    }

    @Override
    public DataType getType() {
        return DataType.ARRAY;
    }

    public RESPObject[] getValues() {
        return this.values;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("*" + values.length + "\r\n");
        for (RESPObject value : values) {
            sb.append(value.toString());
        }
        return sb.toString();
    }

}
