package store;

import Parser.RedisTypes.RESPArray;
import Parser.RedisTypes.RESPBulkString;
import Parser.RedisTypes.RESPObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;

public class Replica {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private int currentOffset = 0;
    private int desiredOffset = 0;
    private final static int REPLCONFGETACKSIZE = 37;

    public Replica(Socket socket, BufferedWriter writer) throws IOException {
        this.socket = socket;
        this.writer = writer;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public Socket getSocket() {
        return socket;
    }

    public void setCurrentOffset(int currentOffset) {
        this.currentOffset = currentOffset;
    }

    public void passCommand(List<RESPObject> commandArr) throws IOException {
        String command = new RESPArray(commandArr).toRedisString();
        desiredOffset += command.length();
        writer.write(command);
        writer.flush();
    }

    public void sendAck() throws IOException {
        List<RESPObject> commandArr = List.of(new RESPBulkString("REPLCONF"), new RESPBulkString("GETACK"), new RESPBulkString("*"));
        String command = new RESPArray(commandArr).toRedisString();
        desiredOffset += command.length();
        writer.write(command);
        writer.flush();
    }

    public boolean isSyncedWithMaster() {
        return desiredOffset - REPLCONFGETACKSIZE <= currentOffset;
    }
}
