package Parser;

import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPBulkString;
import Parser.RESTObjects.RESPObject;
import RdbParser.KeyValuePair;
import store.Cache;
import store.Replicas;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.*;

public class CommandExecutor {

    public String execute(RESPArray command, Cache cache, Map<String, String> config, Replicas replicas) throws IOException {
        RESPObject[] items = command.getValues();
        if (items.length == 0) {
            return "-Err Empty command\r\n";
        }

        RESPBulkString commandName = (RESPBulkString) items[0];
        String commandString = commandName.getValue().toUpperCase();
        ArrayList<KeyValuePair> db = cache.getCache();

        return switch (Commands.valueOf(commandString)) {
            case Commands.PING -> "+PONG\r\n";
            case Commands.ECHO -> {
                RESPBulkString ArgName = (RESPBulkString) items[1];
                yield ArgName.toString();
            }
            case Commands.SET -> executeSetCommand(items, cache, replicas);
            case Commands.GET -> executeGetCommand(items, cache);
            case Commands.CONFIG -> executeConfigGetCommand(items, config);
            case Commands.KEYS -> executeKeysCommand(items, db);
            case Commands.INFO -> executeInfoCommand(items, config);
            case Commands.REPLCONF -> executeReplConfCommand(items, cache);
            case Commands.PSYNC -> executePsyncCommand(items, config);
            case Commands.WAIT -> executeWaitCommand(items);
            default -> "-ERR Unknown command\r\n";
        };
    }

    private String executeSetCommand(RESPObject[] items, Cache cache, Replicas replicas) throws IOException {
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
        cache.setCache(entry);
        StringBuilder replicateCommand = new StringBuilder();
        replicateCommand.append("*").append(itemValues.size()).append("\r\n");
        for (String c : itemValues) {
            replicateCommand.append("$").append(c.length()).append("\r\n").append(c).append("\r\n");
        }

        replicas.getReplicas().forEach(outputStream -> {
            try {
                outputStream.write(replicateCommand.toString().getBytes());
                outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return "+OK\r\n";
    }

    private String executeGetCommand(RESPObject[] items, Cache cache) {
        if (items.length < 2) {
            return "-Err incorrect number of arguments for 'get' command\r\n";
        }

        String key = ((RESPBulkString) items[1]).getValue();
        KeyValuePair entry = cache.getCache().stream().filter(item -> item.getKey().equals(key)).toList().get(0);
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

    private String executeInfoCommand(RESPObject[] items, Map<String, String> config) {
        List<String> itemValues = extractItemValuesFromRespObjects(items);
        String infoArgument = "";
        if (itemValues.size() > 1) {
            infoArgument = itemValues.get(1).toUpperCase();
        }

        return switch (infoArgument) {
            case "REPLICATION" -> {
                boolean isSlave = config.containsKey("masterHost");
                String type = isSlave ? "role:slave" : "role:master";
                String masterReplid = "master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
                String masterReplOffset = "master_repl_offset:0";
                String res = masterReplOffset + masterReplid + type;
                yield "$" + res.length() + "\r\n" + res + "\r\n";
            }
            default -> "Not reached";
        };
    }

    private String executePsyncCommand(RESPObject[] items, Map<String, String> config) throws IOException {
        if (items.length < 3) {
            return "-Err Invalid number of arguments for 'psync' command";
        }
        List<String> itemValues = extractItemValuesFromRespObjects(items);
        String replID = itemValues.get(1);
        String offset = itemValues.get(2);

        return "+FULLRESYNC 8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb 0 \r\n";
    }

    private String executeReplConfCommand(RESPObject[] items, Cache cache) {
        if (items.length < 3) {
            return "-Err Invalid number of commands for 'replconf command'";
        }

        List<String> commands = extractItemValuesFromRespObjects(items);
        String commandArgument = commands.get(1);
        String cacheOffset = Integer.toString(cache.getOffset());

        return switch (commandArgument) {
            case "listening-port", "capa" -> "+OK\r\n";
            case "GETACK" -> "*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$" + cacheOffset.length() + "\r\n" + cacheOffset + "\r\n";
            default -> "";
        };
    }

    private String executeWaitCommand(RESPObject[] items) {
        if (items.length < 3) {
            return "-Err Invalid number of arguments for 'WAIT' command";
        }

        List<String> commandArguments = extractItemValuesFromRespObjects(items);

        return ":0\r\n";
    }

    private List<String> extractItemValuesFromRespObjects(RESPObject[] items) {
        return Arrays.stream(items).map(i -> ((RESPBulkString) i).getValue()).toList();
    }
}
