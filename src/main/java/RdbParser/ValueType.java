package RdbParser;

public enum ValueType {
    STRING(0, "string"),
    LIST(1, "list"),
    SET(2, "set"),
    SORTED_SET(3, "zset"),
    HASH(4, "hash"),
    ZIPMAP(9, ""),
    ZIPLIST(10, ""),
    INTSET(11, ""),
    SORTED_SET_IN_ZIPLIST(12, ""),
    HASHMAP_IN_ZIPLIST(13, ""),
    LIST_IN_QUICKLIST(14, "");

    private final int type;
    private final String typeName;
    ValueType(int type, String typeName) {
        this.type = type;
        this.typeName = typeName;
    }

    public int getType() { return type; }
    public String getTypeName() { return typeName; }

    public static ValueType getTypeFromInt(int t) {
        for (ValueType i : ValueType.values()) {
            if (i.getType() == t) {
                return i;
            }
        }

        throw new IllegalArgumentException("Unknown Value Type " + t);
    }
}


