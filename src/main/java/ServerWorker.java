

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerWorker implements Runnable {
    private final ServerSocket ss;

    public ServerWorker(ServerSocket ss) {
        this.ss = ss;
    }

    @Override
    public void run() {
        try {
            for(;;) {
                Socket sock = ss.accept();
                Thread th = new Thread(new SocketWorker(sock));
                th.setDaemon(true);
                th.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
