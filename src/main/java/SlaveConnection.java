import Parser.Parser;
import Parser.RedisTypes.RESPArray;
import Parser.RedisTypes.RESPObject;
import Parser.RedisCommandExecutor;
import store.RedisCache;
import store.RedisReplicas;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class SlaveConnection implements Runnable {
    Socket slave;
    Map<String, String> config;
    RedisCache db;
    RedisReplicas replicas;

    public SlaveConnection(Socket socket, Map<String, String> config, RedisCache db, RedisReplicas replicas) {
        this.slave = socket;
        this.config = config;
        this.db = db;
        this.replicas = replicas;
    }

    public void run() {
        try {
            OutputStream outputStream = slave.getOutputStream();
            InputStream inputStream = slave.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            writer.write("*1\r\n$4\r\nPING\r\n");
            writer.flush();
            reader.readLine();
            writer.write("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n6380\r\n");
            writer.flush();
            reader.readLine();
            writer.write("*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n");
            writer.flush();
            reader.readLine();
            writer.write("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n");
            writer.flush();
            reader.readLine(); // read FULLRESYNC

            processEmptyRDBFile(reader);
            while (true) {
                Parser parser = new Parser(reader);
                RESPObject command = parser.parse();
                if (command == null) {
                    continue;
                }
                RedisCache.setCurrOffset();
                RedisCache.setOffset(command.toRedisString().length());
                String response = new RedisCommandExecutor(slave, writer, config).execute((RESPArray) command);
                if (response.contains("ACK")) {
                    writer.write(response);
                    writer.flush();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processEmptyRDBFile(BufferedReader reader) throws IOException {
        if (reader.read() == -1) {
            return;
        }
        int length = Integer.parseInt(reader.readLine());
        char[] data = new char[length-1];
        reader.read(data);
    }
}
