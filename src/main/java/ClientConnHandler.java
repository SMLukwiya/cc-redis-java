import Parser.Parser;
import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPObject;
import Parser.CommandExecutor;
import store.Cache;
import store.Replicas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;

public class ClientConnHandler implements Runnable {
    private static String emptyRDBFileContent = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
    private final Socket socket;
    Cache db;
    Map<String, String> config;
    Replicas replicas;

    public ClientConnHandler(Socket socket, Cache db, Map<String, String> config, Replicas replicas) {
        this.socket = socket;
        this.db = db;
        this.config = config;
        this.replicas = replicas;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                Parser parser = new Parser(reader);
                RESPObject value = parser.parse();
                OutputStream outputStream = socket.getOutputStream();
                if (value instanceof RESPArray) {
                    String argument = new CommandExecutor().execute((RESPArray) value, db, config, replicas);
                    outputStream.write(argument.getBytes());

                    if (argument.contains("FULLRESYNC")) {
                        sendEmptyRDBFile(outputStream);
                        replicas.setReplica(outputStream);
                    }
                    outputStream.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }

    private void sendEmptyRDBFile(OutputStream os) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(emptyRDBFileContent);
        os.write(("$" + bytes.length + "\r\n").getBytes());
        os.write(bytes);
    }
}
