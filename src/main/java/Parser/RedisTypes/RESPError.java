package Parser.RedisTypes;

public record RESPError(String error) implements RESPObject {
    @Override
    public String toRedisString() {
        return "-" + error + "\r\n";
    }
}
