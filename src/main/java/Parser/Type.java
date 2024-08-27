package Parser;

public class Type {
    final DataType type;
    final Object literal;

    public Type(DataType type, Object literal) {
        this.type = type;
        this.literal = literal;
    }
}
