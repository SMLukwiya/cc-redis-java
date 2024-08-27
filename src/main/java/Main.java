import Parser.Parser;
import Parser.RESTObjects.RESPArray;
import Parser.RESTObjects.RESPObject;
import Parser.CommandExecutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

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

//          new NetworkService(port, 4);

          try {
            for (;;) {
                clientSocket = serverSocket.accept();
                pool.execute(new MultiResponse(clientSocket));
                System.out.println("POOL => " + pool.isShutdown());
            }
          } catch (IOException ex) {
              System.out.println(("IOException => " + ex.getMessage()));
          }
//            while (true) {
//                clientSocket = serverSocket.accept();
//                Socket finalClientSocket = clientSocket;
//                new Thread(new MultiResponse(clientSocket)).start();
//            }
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        }
  }

  static class NetworkService implements Runnable {
      private final ServerSocket serverSocket;
      private final ExecutorService pool;

      public NetworkService(int port, int poolSize)
        throws IOException {
          serverSocket = new ServerSocket(port);
          pool = Executors.newFixedThreadPool(poolSize);
      }

      public void run() { // run the service
          try {
              for (;;) {
                  System.out.println("RUnning");
                  pool.execute(new MultiResponse(serverSocket.accept()));
              }
          } catch (IOException ex) {
              pool.shutdown();
          }
      }
  }

  static class MultiResponse implements Runnable {
      private final Socket socket;

      public MultiResponse(Socket socket) {
          this.socket = socket;
      }

      public void run() {
          try {
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
              Parser parser = new Parser(reader);
              RESPObject value = parser.parse();
              if (value instanceof RESPArray) {
                  String argument = new CommandExecutor().execute((RESPArray) value);
                  socket.getOutputStream().write(argument.getBytes());
                  socket.getOutputStream().flush();
              }
              String input;
              while ((input = reader.readLine()) != null) {
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
