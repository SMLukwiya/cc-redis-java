package Parser;

import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPBulkString;
import Parser.RESTObjects.RESPObject;
import RdbParser.KeyValuePair;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class CommandExecutor {
    public String execute(RESPArray command, ArrayList<KeyValuePair> db, Map<String, String> config, OutputStream os, BlockingQueue<String> commandQueue) throws IOException, InterruptedException {
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
                return executeSetCommand(items, db, os, commandQueue);
            case Commands.GET:
                return executeGetCommand(items, db);
            case Commands.CONFIG:
                return executeConfigGetCommand(items, config);
            case Commands.KEYS:
                return executeKeysCommand(items, db);
            case Commands.INFO:
                return executeInfoCommand(items, config);
            case Commands.REPLCONF:
                return "+OK\r\n";
            case Commands.PSYNC:
                return executePsyncCommand(items, config, os, commandQueue);
            default:
                return "-ERR Unknown command\r\n";
        }
    }

    private String executeSetCommand(RESPObject[] items, ArrayList<KeyValuePair> db, OutputStream os, BlockingQueue<String> queue) throws IOException {
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
        StringBuilder replicateCommand = new StringBuilder();
        replicateCommand.append("*").append(itemValues.size()).append("\r\n");
        for (String c : itemValues) {
            replicateCommand.append("$").append(c.length()).append("\r\n").append(c).append("\r\n");
        }

        queue.add(replicateCommand.toString());
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

    private String executePsyncCommand(RESPObject[] items, Map<String, String> config, OutputStream os, BlockingQueue<String> queue) throws IOException, InterruptedException {
        if (items.length < 3) {
            return "-Err Invalid number of arguments for 'psync' command";
        }
        List<String> itemValues = extractItemValuesFromRespObjects(items);
        String replID = itemValues.get(1);
        String offset = itemValues.get(2);

        os.write("+FULLRESYNC 8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb 0 \r\n".getBytes());
        String emptyRDBFileContent = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
        byte[] bytes = Base64.getDecoder().decode(emptyRDBFileContent);
        os.write(("$" + bytes.length + "\r\n").getBytes());
        os.write(bytes);
        //
        while (true) {
            String command = queue.take();
            os.write(command.getBytes());
        }
//        return null;
    }

    private List<String> extractItemValuesFromRespObjects(RESPObject[] items) {
        return Arrays.stream(items).map(i -> ((RESPBulkString) i).getValue()).toList();
    }

    // utils
    private String base64ToBinary(String base64String) {
        StringBuilder result = new StringBuilder();
        byte[] bytes = Base64.getDecoder().decode(base64String);

        for (byte b : bytes) {
            String binary = String.format("%8s", Integer.toBinaryString(b & 0xff).replace(' ', '0'));
            result.append(binary);
        }
        return result.toString();
    }
}
