import RdbParser.RdbParser;
import RdbParser.KeyValuePair;
import store.RedisCache;
import store.RedisReplicas;
import utils.Utils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  public static void main(String[] args){
      System.out.println("Logs from your program will appear here!");
      Map<String, String> config = new HashMap<>();

      ServerSocket serverSocket = null;
      Socket clientSocket = null;
      config = new Utils().setConfig(args, config);
      int port = config.get("port") != null ? Integer.parseInt(config.get("port")) : 6379;

      try {
          String rdbFilePath = config.get("dir") + "/" + config.get("dbfilename");
          File f = new File(rdbFilePath);

          if (f.exists()) {
              DataInputStream dataStream = new DataInputStream(new FileInputStream(rdbFilePath));

              RdbParser rdbParser = new RdbParser(dataStream);
              ArrayList<KeyValuePair> data =  rdbParser.parse();
              for (KeyValuePair d : data) {
                  RedisCache.setCache(d);
              }
              dataStream.close();
          }

          serverSocket = new ServerSocket(port);
          // Since the tester restarts your program quite often, setting SO_REUSEADDR
          // ensures that we don't run into 'Address already in use' errors
          serverSocket.setReuseAddress(true);
          boolean isSlave = Boolean.parseBoolean(config.get("isSlave"));
          if (isSlave) {
              Socket socket = new Socket(config.get("masterHost"), Integer.parseInt(config.get("masterPort")));
              new Thread(new SlaveConnection(socket, config)).start();
          }

          while (true) {
              clientSocket = serverSocket.accept();
              new Thread(new ClientConnHandler(clientSocket, config, isSlave)).start();
          }
      } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
      }
  }
}
