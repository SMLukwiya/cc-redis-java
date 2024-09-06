import Parser.Parser;
import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPObject;
import Parser.CommandExecutor;
import store.Cache;
import store.Replicas;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class SlaveConnection implements Runnable {
    Socket slave;
    Map<String, String> config;
    Cache db;
    Replicas replicas;

    public SlaveConnection(Socket socket, Map<String, String> config, Cache db, Replicas replicas) {
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

            outputStream.write("*1\r\n$4\r\nPING\r\n".getBytes());
            reader.readLine();
            outputStream.write("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n6380\r\n".getBytes());
            reader.readLine();
            outputStream.write("*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n".getBytes());
            reader.readLine();
            outputStream.write("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n".getBytes());
            reader.readLine(); // read FULLRESYNC

            processEmptyRDBFile(reader);
            while (true) {
                Parser parser = new Parser(reader);
                RESPObject value = parser.parse();
                if (value == null) {
                    break;
                }
                db.setCurrOffset();
                db.setOffset(value.toString().length());
                String response = new CommandExecutor().execute((RESPArray) value, db, config, replicas);
                if (response.contains("ACK")) {
                    outputStream.write(response.getBytes());
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
