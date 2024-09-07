package Parser.RedisTypes;

public record RESPBulkString(String value) implements RESPObject {
    @Override
    public String toRedisString() {
        return "$" + value.length() + "\r\n" + value + "\r\n";
    }
}
