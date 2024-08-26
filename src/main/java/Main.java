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

            for (;;) {
                clientSocket = serverSocket.accept();
                pool.execute(new MultiResponse(clientSocket));
            }
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        }
  }

  static class MultiResponse implements Runnable {
      Socket socket;

      public MultiResponse(Socket socket) {
          this.socket = socket;
      }

      public void run() {
          try {
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
