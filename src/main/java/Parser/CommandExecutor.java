package Parser;

import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPBulkString;
import Parser.RESTObjects.RESPObject;

public class CommandExecutor {
    public String execute(RESPArray command) {
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
            default:
                return "-ERR Unknown command\r\n";
        }
    }
}
