

import java.io.IOException;

public class Launcher {
    public static void main(String[] args) throws IOException {
        if (1 != args.length) {
            System.err.println("ERROR: port number must be specified as a first and only argument");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        Server server = new Server(port);
        server.start();
        System.out.println("Server started, port: [" + port + "]");
        System.out.println("Press Enter to stop ...");
        System.console().readLine();
        System.out.println("Shutting down ...");
        //server.stop();
    }
}
