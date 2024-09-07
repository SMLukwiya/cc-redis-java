import Parser.Parser;
import Parser.RedisTypes.RESPArray;
import Parser.RedisTypes.RESPObject;
import Parser.RedisCommandExecutor;
import store.RedisCache;
import store.RedisReplicas;

import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;

public class ClientConnHandler implements Runnable {
    private static String emptyRDBFileContent = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
    private final Socket socket;
    RedisCache db;
    Map<String, String> config;
    RedisReplicas replicas;
    boolean readAfterHandShake = false;

    public ClientConnHandler(Socket socket, RedisCache db, Map<String, String> config, RedisReplicas replicas, boolean readAfterHandShake) {
        this.socket = socket;
        this.db = db;
        this.config = config;
        this.replicas = replicas;
        this.readAfterHandShake = readAfterHandShake;

        if (readAfterHandShake) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            while (true) {
                Parser parser = new Parser(reader);
                RESPObject command = parser.parse();
                OutputStream outputStream = socket.getOutputStream();

                if (command instanceof RESPArray) {
                    String argument = new RedisCommandExecutor(socket, writer, config).execute((RESPArray) command);
                    if (argument == null) {
                        continue;
                    }
                    writer.write(argument);

                    if (argument.contains("FULLRESYNC")) {
                        sendEmptyRDBFile(writer, outputStream);
                        RedisReplicas.setReplica(socket, writer);
                    }
                    writer.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
//            try {
//                if (socket != null) {
//                    socket.close();
//                }
//            } catch (IOException e) {
//                System.out.println("IOException: " + e.getMessage());
//            }
        }
    }

    private void sendEmptyRDBFile(BufferedWriter writer, OutputStream os) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(emptyRDBFileContent);
        writer.write(("$" + bytes.length + "\r\n"));
        writer.flush();
        os.write(bytes);
    }
}
