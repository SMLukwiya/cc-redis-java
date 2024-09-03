import Parser.Parser;
import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPObject;
import Parser.CommandExecutor;
import Parser.RESTObjects.RespValue;
import RdbParser.RdbParser;
import RdbParser.KeyValuePair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");
    ArrayList<KeyValuePair> db = new ArrayList<>();
    Map<String, String> config = new HashMap<>();

    for (int i = 0; i < args.length; i++) {
        switch(args[i]) {
            case "--dir":
                if (i+1 < args.length) {
                    config.put("dir", args[i+1]);
                }
                break;
            case "--dbfilename":
                if (i+1 < args.length) {
                    config.put("dbfilename", args[i+1]);
                }
                break;
            case "--port":
                if (i+1 < args.length) {
                    config.put("port", args[i+1]);
                }
        }
    }

    //  Uncomment this block to pass the first stage
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = config.get("port") != null ? Integer.parseInt(config.get("port")) : 6379;
        final ExecutorService pool;

        try {
            String rdbFilePath = config.get("dir") + "/" + config.get("dbfilename");
            File f = new File(rdbFilePath);

            if (f.exists()) {
                DataInputStream dataStream = new DataInputStream(new FileInputStream(rdbFilePath));

                RdbParser rdbParser = new RdbParser(dataStream);
                db = rdbParser.parse();
                dataStream.close();
            }

          serverSocket = new ServerSocket(port);
          pool = Executors.newFixedThreadPool(4);
          // Since the tester restarts your program quite often, setting SO_REUSEADDR
          // ensures that we don't run into 'Address already in use' errors
          serverSocket.setReuseAddress(true);

          try {
            for (;;) {
                clientSocket = serverSocket.accept();
                pool.execute(new MultiResponse(clientSocket, db, config));
            }
          } catch (IOException ex) {
              System.out.println(("IOException => " + ex.getMessage()));
          }
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        }
  }

  static class MultiResponse implements Runnable {
      private final Socket socket;

      private final ArrayList<KeyValuePair> db;
      Map<String, String> config;

      public MultiResponse(Socket socket, ArrayList<KeyValuePair> db, Map<String, String> config) {
          this.socket = socket;
          this.db = db;
          this.config = config;
      }

      public void run() {
          try {
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
              while (true) {
                  Parser parser = new Parser(reader);
                  RESPObject value = parser.parse();
                  if (value instanceof RESPArray) {
                      String argument = new CommandExecutor().execute((RESPArray) value, db, config);
                      socket.getOutputStream().write(argument.getBytes());
                      socket.getOutputStream().flush();
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
  }
}
