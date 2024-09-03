package Parser;

import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPBulkString;
import Parser.RESTObjects.RESPObject;
import RdbParser.KeyValuePair;

import java.sql.Timestamp;
import java.util.*;

public class CommandExecutor {
    public String execute(RESPArray command, ArrayList<KeyValuePair> db, Map<String, String> config) {
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
            case Commands.CONFIG:
                return executeConfigGetCommand(items, config);
            case Commands.KEYS:
                return executeKeysCommand(items, db);
            case Commands.INFO:
                return executeInfoCommand(items);
            default:
                return "-ERR Unknown command\r\n";
        }
    }

    private String executeSetCommand(RESPObject[] items, ArrayList<KeyValuePair> db) {
        if (items.length < 3) {
            return "-Err incorrect number of arguments for 'set' command\r\n";
        }
        boolean hasExtraProps = items.length > 3;
        if (hasExtraProps && items.length - 3 < 2) {
            return "-Err incorrect number of arguments for ;'set' command\r\n";
        }

        List<String> itemValues = extractItemValuesFromRespObjects(items);

        String key = itemValues.get(1);
        String value = itemValues.get(2);

        KeyValuePair entry = new KeyValuePair();
        entry.setValue(value);
        entry.setKey(key);

        if (items.length > 3) {
            long expiryValue = new Date().getTime() + Long.parseLong(itemValues.get(4));
            entry.setExpiryTime(new Timestamp(expiryValue));
        }
        db.add(entry);

        return "+OK\r\n";
    }

    private String executeGetCommand(RESPObject[] items, List<KeyValuePair> db) {
        if (items.length < 2) {
            return "-Err incorrect number of arguments for 'get' command\r\n";
        }

        String key = ((RESPBulkString) items[1]).getValue();
        KeyValuePair entry = db.stream().filter(item -> item.getKey().equals(key)).toList().get(0);
        boolean hasExpired = false;

        if (entry.getExpiryTime() != null) {
            hasExpired = entry.getExpiryTime().getTime() < new Date().getTime();
        }
        return (entry == null || hasExpired) ? "$-1\r\n" : "$" + entry.getValue().toString().length() + "\r\n" + entry.getValue() + "\r\n";
    }

    private String executeConfigGetCommand(RESPObject[] items, Map<String, String> config) {
        if (items.length < 3) {
            return "-Err invalid number of parameters for 'config get' command";
        }
        List<String> itemValues = extractItemValuesFromRespObjects(items);
        String key = itemValues.get(2);

        if (key.equals("dir")) {
            String dir = config.get("dir");
            return "*2\r\n$3\r\ndir\r\n$" + dir.length() + "\r\n" + dir + "\r\n";
        }

        if (key.equals("dbfilename")) {
            String dbFileName = config.get("dbfilename");
            return "*2\r\n$10\r\ndbfilename\r\n$" + dbFileName.length() + "\r\n" + dbFileName + "\r\n";
        }

        return "$-1\r\n";
    }

    private String executeKeysCommand(RESPObject[] items, List<KeyValuePair> data) {
        if (items.length < 2) {
            return "-Err Invalid number of parameters for 'keys' command";
        }
        String keyName = ((RESPBulkString) items[1]).getValue();
        List<String> keys = data.stream().map(KeyValuePair::getKey).toList();

        if (keyName.equals("*")) {
            StringBuilder result = new StringBuilder("*" + keys.size() + "\r\n");
            for (String k : keys) {
                result.append("$").append(k.length()).append("\r\n").append(k).append("\r\n");
            }
            return result.toString();
        } else {
            List<String> res = keys.stream().filter(i -> i.equals(keyName)).toList();
            return res.get(0);
        }
    }

    private String executeInfoCommand(RESPObject[] items) {
        List<String> itemValues = extractItemValuesFromRespObjects(items);
        String infoArgument = "";
        if (itemValues.size() > 1) {
            infoArgument = itemValues.get(1).toUpperCase();
        }

        return switch (infoArgument) {
            case "REPLICATION" -> "$11\r\nrole:master\r\n";
            default -> "Not reached";
        };
    }

    private List<String> extractItemValuesFromRespObjects(RESPObject[] items) {
        return Arrays.stream(items).map(i -> ((RESPBulkString) i).getValue()).toList();
    }
}
