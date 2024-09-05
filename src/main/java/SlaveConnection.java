import Parser.Parser;
import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPObject;
import Parser.CommandExecutor;
import RdbParser.KeyValuePair;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

public class SlaveConnection {
    Socket slave;
    Map<String, String> config;
    ArrayList<KeyValuePair> db;

    public SlaveConnection(Map<String, String> config, ArrayList<KeyValuePair> db) {
        this.config = config;
        this.db = db;
    }

    public void initiate() throws IOException {
        this.slave = new Socket(config.get("masterHost"), Integer.parseInt(config.get("masterPort")));

        OutputStream outputStream = slave.getOutputStream();
        InputStream inputStream = slave.getInputStream();
        processEmptyRDBFile(inputStream);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        while (true) {
            outputStream.write("*1\r\n$4\r\nPING\r\n".getBytes());
            reader.readLine();
            outputStream.write("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n6380\r\n".getBytes());
            reader.readLine();
            outputStream.write("*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n".getBytes());
            reader.readLine();
            outputStream.write("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n".getBytes());

            Parser parser = new Parser(reader);
            RESPObject value = parser.parse();

            if (value instanceof RESPArray) {
                String argument = new CommandExecutor().executeReplica((RESPArray) value, db, config);
            }
            outputStream.flush();
        }
    }

    private void processEmptyRDBFile(InputStream is) throws IOException {
        byte[] b = new byte[5];
        is.read(b);
        String length = new String(b, Charset.defaultCharset());
        System.out.println("Length of file ===> " + length);
    }
}
