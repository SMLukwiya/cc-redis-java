package Parser.RedisTypes;

public record RESPNull() implements RESPObject {

    @Override
    public String toRedisString() {
        return "$-1\r\n";
    }
}
