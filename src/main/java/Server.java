

import java.io.IOException;
import java.net.ServerSocket;

public class Server {

    private final ServerSocket ss;

    public Server(int port) throws IOException {
        this.ss = new ServerSocket(port);
    }

    public void start() {
        Thread thread = new Thread(new ServerWorker(ss));
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        try {
            ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
