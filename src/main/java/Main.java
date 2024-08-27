import Parser.Parser;
import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPObject;
import Parser.CommandExecutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");
    Map<String, Object> db = new HashMap<>();

    //  Uncomment this block to pass the first stage
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        final ExecutorService pool;

        try {
          serverSocket = new ServerSocket(port);
          pool = Executors.newFixedThreadPool(4);
          // Since the tester restarts your program quite often, setting SO_REUSEADDR
          // ensures that we don't run into 'Address already in use' errors
          serverSocket.setReuseAddress(true);

          try {
            for (;;) {
                clientSocket = serverSocket.accept();
                pool.execute(new MultiResponse(clientSocket, db));
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
      Map<String, Object> db;

      public MultiResponse(Socket socket, Map<String, Object> db) {
          this.socket = socket;
          this.db = db;
      }

      public void run() {
          try {
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
              System.out.println("INPUTsss => " + reader.readLine());
              Parser parser = new Parser(reader);
              RESPObject value = parser.parse();
              if (value instanceof RESPArray) {
                  String argument = new CommandExecutor().execute((RESPArray) value, db);
                  socket.getOutputStream().write(argument.getBytes());
                  socket.getOutputStream().flush();
              }
              String input;
              while (reader.ready()) {
                  System.out.println("INPUT => " + reader.readLine());
              }
              while ((input = reader.readLine()) != null) {
//                  System.out.println("INPUT => " + input);
                  if (input.startsWith("PING")) {
                      socket.getOutputStream().write("+PONG\r\n".getBytes());
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
