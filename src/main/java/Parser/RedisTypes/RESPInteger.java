package Parser.RedisTypes;

public record RESPInteger(int num) implements RESPObject {
    @Override
    public String toRedisString() {
        return ":" + num + "\r\n";
    }
}
