package RdbParser;

public enum ValueType {
    STRING(0),
    LIST(1),
    SET(2),
    SORTED_SET(3),
    HASH(4),
    ZIPMAP(9),
    ZIPLIST(10),
    INTSET(11),
    SORTED_SET_IN_ZIPLIST(12),
    HASHMAP_IN_ZIPLIST(13),
    LIST_IN_QUICKLIST(14);

    private final int type;
    ValueType(int type) { this.type = type; }

    public int getType() { return type; }

    public static ValueType getTypeFromInt(int t) {
        for (ValueType i : ValueType.values()) {
            if (i.getType() == t) {
                return i;
            }
        }

        throw new IllegalArgumentException("Unknown Value Type " + t);
    }
}


