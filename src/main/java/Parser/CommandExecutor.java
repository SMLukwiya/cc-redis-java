package Parser;

import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPBulkString;
import Parser.RESTObjects.RESPObject;
import Parser.RESTObjects.RespValue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandExecutor {
    public String execute(RESPArray command, Map<String, RespValue> db) {
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

    private String executeSetCommand(RESPObject[] items, Map<String, RespValue> db) {
        if (items.length < 3) {
            return "-Err incorrect number of arguments for 'set' command\r\n";
        }
        boolean hasExtraProps = items.length > 3;
        if (hasExtraProps && items.length - 3 < 2) {
            return "-Err incorrect number of arguments for ;'set' command\r\n";
        }

        List<String> itemValues = Arrays.stream(items)
          .map(respObject -> ((RESPBulkString) respObject).getValue())
          .toList();

        String key = itemValues.get(1);
        String value = itemValues.get(2);
        long expiryValue;
        if (items.length > 3) {
            expiryValue = new Date().getTime() + Long.parseLong(itemValues.get(4));
        } else {
            expiryValue = new Date().getTime() + 100000;
        }

        db.put(key, new RespValue(value, expiryValue));
        return "+OK\r\n";
    }

    private String executeGetCommand(RESPObject[] items, Map<String, RespValue> db) {
        if (items.length < 2) {
            return "-Err incorrect number of arguments for 'get' command\r\n";
        }

        String key = ((RESPBulkString) items[1]).getValue();
        RespValue value = db.get(key);
        System.out.println("EXPIRY => " + value.expiry() + " => " + new Date().getTime());
        boolean hasExpired = value.expiry() < new Date().getTime();
        return (value == null || hasExpired) ? "$-1\r\n" : "$" + value.value().toString().length() + "\r\n" + value.value().toString() + "\r\n";
    }
}
