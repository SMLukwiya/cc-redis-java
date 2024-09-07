package Parser.RedisTypes;

public record RESPSimpleString(String value) implements RESPObject {
    @Override
    public String toRedisString() {
        return "+" + this.value + "\r\n";
    }
}
