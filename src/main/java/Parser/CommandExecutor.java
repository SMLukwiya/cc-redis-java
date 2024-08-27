package Parser;

import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPBulkString;
import Parser.RESTObjects.RESPObject;

import java.util.Map;

public class CommandExecutor {
    public String execute(RESPArray command, Map<String, Object> db) {
        RESPObject[] items = command.getValues();
        if (items.length == 0) {
            return "-Err Empty command\r\n";
        }

        RESPBulkString commandName = (RESPBulkString) items[0];
        String commandString = commandName.getValue().toUpperCase();

        switch (Commands.valueOf(commandString)) {
            case Commands.PING:
                return "+PONG\r\n";
            case Commands.ECHO:
                RESPBulkString ArgName = (RESPBulkString) items[1];
                return ArgName.toString();
            case Commands.SET:
                return executeSetCommand(items, db);
            case Commands.GET:
                return executeGetCommand(items, db);
            default:
                return "-ERR Unknown command\r\n";
        }
    }

    private String executeSetCommand(RESPObject[] items, Map<String, Object> db) {
        if (items.length < 3) {
            return "-Err incorrect number of arguments for 'set' command\r\n";
        }

        String key = ((RESPBulkString) items[1]).getValue();
        String value = ((RESPBulkString) items[2]).getValue();

        db.put(key, value);
        return "+OK\r\n";
    }

    private String executeGetCommand(RESPObject[] items, Map<String, Object> db) {
        if (items.length < 2) {
            return "-Err incorrect number of arguments for 'get' command\r\n";
        }

        String key = ((RESPBulkString) items[1]).getValue();
        Object value = db.get(key);
        return value == null ? "+-1\r\n" : "$" + value.toString().length() + "\r\n" + value.toString() + "\r\n";
    }
}
