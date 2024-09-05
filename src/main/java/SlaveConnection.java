import java.io.*;
import java.net.Socket;
import java.util.Map;

public class SlaveConnection {
    Socket slave;
    Map<String, String> config;

    public SlaveConnection(Map<String, String> config) {
        this.config = config;
    }

    public void initiate() throws IOException {
        this.slave = new Socket(config.get("masterHost"), Integer.parseInt(config.get("masterPort")));

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
        outputStream.flush();
    }
}
