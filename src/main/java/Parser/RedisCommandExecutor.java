package Parser;

import Parser.RedisTypes.*;
import RdbParser.KeyValuePair;
import store.RedisCache;
import store.RedisReplicas;
import store.Replica;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.*;

public class RedisCommandExecutor {
    Socket socket;
    BufferedWriter writer;
    RedisCache cache;
    Map<String, String> config;

    public RedisCommandExecutor(Socket socket, BufferedWriter writer, Map<String, String> config) {
        this.socket = socket;
        this.writer = writer;
        this.config = config;
    }

    public String execute(RESPArray command) throws IOException {
        List<RESPObject> items = command.values();
        if (items.isEmpty()) {
            return "-Err Empty command\r\n";
        }

        String redisCommand = ((RESPBulkString) items.getFirst()).value();
        String commandName = redisCommand.toUpperCase();

        return switch (Commands.valueOf(commandName)) {
            case Commands.PING -> new RESPSimpleString("PONG").toRedisString();
            case Commands.ECHO -> items.get(1).toRedisString();
            case Commands.SET -> executeSet(items);
            case Commands.GET -> executeGet(items);
            case Commands.CONFIG -> executeConfig(items);
            case Commands.KEYS -> executeKeys(items);
            case Commands.INFO -> executeInfo(items);
            case Commands.REPLCONF -> executeReplConf(items);
            case Commands.PSYNC -> executePsync(items);
            case Commands.WAIT -> executeWait(items);
            default -> "-ERR Unknown command\r\n";
        };
    }

    private String executeSet(List<RESPObject> command) {
        if (command.size() < 3) {
            return "-Err incorrect number of arguments for 'set' command\r\n";
        }
        boolean hasExtraArgs = command.size() > 3;

        String key = ((RESPBulkString) command.get(1)).value();
        String value = ((RESPBulkString) command.get(2)).value();

        KeyValuePair entry = new KeyValuePair();
        entry.setValue(value);
        entry.setKey(key);

        if (hasExtraArgs) {
            String expiry = ((RESPBulkString) command.get(4)).value();
            long expiryValue = new Date().getTime() + Long.parseLong(expiry);
            entry.setExpiryTime(new Timestamp(expiryValue));
        }
        RedisCache.setCache(entry);
        RedisReplicas.propagateCommand(command);
        return new RESPSimpleString("OK").toRedisString();
    }

    private String executeGet(List<RESPObject> command) {
        if (command.size() < 2) {
            return "-Err incorrect number of arguments for 'get' command\r\n";
        }

        String key = ((RESPBulkString) command.get(1)).value();
        KeyValuePair entry = RedisCache.getCache().stream().filter(item -> item.getKey().equals(key)).toList().get(0);
        boolean hasExpired = false;

        if (entry.getExpiryTime() != null) {
            hasExpired = entry.getExpiryTime().getTime() < new Date().getTime();
        }
        return (entry == null || hasExpired) ? "$-1\r\n" : "$" + entry.getValue().toString().length() + "\r\n" + entry.getValue() + "\r\n";
    }

    private String executeConfig(List<RESPObject> command) {
        if (command.size() < 3) {
            return "-Err invalid number of parameters for 'config get' command";
        }

        String key = ((RESPBulkString) command.get(2)).value();

        if (key.equals("dir")) {
            String dir = config.get("dir");
            List<RESPObject> response = List.of(new RESPBulkString("dir"), new RESPBulkString(dir));
            return new RESPArray(response).toRedisString();
        }

        if (key.equals("dbfilename")) {
            String dbFileName = config.get("dbfilename");
            List<RESPObject> response = List.of(new RESPBulkString("dbfilename"), new RESPBulkString(dbFileName));
            return new RESPArray(response).toRedisString();
        }

        return new RESPArray(List.of(new RESPBulkString("-1"))).toRedisString();
    }

    private String executeKeys(List<RESPObject> command) {
        if (command.size() < 2) {
            return "-Err Invalid number of parameters for 'keys' command";
        }
        String keyName = ((RESPBulkString) command.get(1)).value();
        List<String> keys = RedisCache.getCache().stream().map(KeyValuePair::getKey).toList();

        if (keyName.equals("*")) {
            List<RESPObject> response = new ArrayList<>();
            for (String k : keys) {
                response.add(new RESPBulkString(k));
            }
            return new RESPArray(response).toRedisString();
        } else {
            String key = keys.stream().filter(i -> i.equals(keyName)).findFirst().orElse(null);
            return new RESPBulkString(key).toRedisString();
        }
    }

    private String executeInfo(List<RESPObject> commands) {
        String infoArgument = "";
        if (commands.size() > 1) {
            infoArgument = ((RESPBulkString) commands.get(1)).value().toUpperCase();
        }

        if (infoArgument.equals("REPLICATION")) {
            boolean isSlave = config.containsKey("masterHost");
            String type = isSlave ? "role:slave" : "role:master";
            String masterReplid = "master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
            String masterReplOffset = "master_repl_offset:0";
            String res = masterReplOffset + masterReplid + type;
            return new RESPBulkString(res).toRedisString();
        };
        return null;
    }

    private String executePsync(List<RESPObject> commands) {
        if (commands.size() < 3) {
            return "-Err Invalid number of arguments for 'psync' command";
        }
        String replID = ((RESPBulkString) commands.get(1)).value();
        String offset = ((RESPBulkString) commands.get(2)).value();

        return "+FULLRESYNC 8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb 0 \r\n";
    }

    private String executeReplConf(List<RESPObject> command) {
        if (command.size() < 3) {
            return "-Err Invalid number of commands for 'replconf command'";
        }

        String commandArg = ((RESPBulkString) command.get(1)).value();
        String cacheOffset = Integer.toString(RedisCache.getOffset());

        return switch (commandArg) {
            case "listening-port", "capa" -> "+OK\r\n";
            case "GETACK" -> "*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$" + cacheOffset.length() + "\r\n" + cacheOffset + "\r\n";
            case "ACK" -> {
                int offset = Integer.parseInt(((RESPBulkString) command.get(2)).value());
                Replica replica = RedisReplicas.findReplica(socket);
                replica.setCurrentOffset(offset);
                yield null;
            }
            default -> null;
        };
    }

    private String executeWait(List<RESPObject> command) {
        if (command.size() < 3) {
            return "-Err Invalid number of arguments for 'WAIT' command";
        }

        int numOfReplicasNeeded = Integer.parseInt(((RESPBulkString) command.get(1)).value());
        int timeTowait = Integer.parseInt(((RESPBulkString) command.get(2)).value());

        int numOfReplicasToAck = RedisReplicas.getAllSyncedReplicas(numOfReplicasNeeded, timeTowait);
        return new RESPInteger(numOfReplicasToAck).toRedisString();
    }
}
