

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SocketWorker implements Runnable {
    private static final String resp1 = "HTTP/1.1 407 Proxy Authentication Required\r\n" +
            "Content-Length: 0\r\n" +
            "Proxy-Authenticate: NTLM\r\n\r\n";
    private static final String resp2 = "HTTP/1.1 407 Proxy Authentication Required\r\n" +
            "Content-Length: 0\r\n" +
            "Proxy-Authenticate: NTLM TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==\r\n\r\n";
    private static final String resp3 = "HTTP/1.1 200 Connection Established\r\n\r\n";

    private enum RequestType {
        CLOSED, NOAUTH, NTLMSPP_NEGOTIATE, NTLMSPP_AUTH
    };

    private final Socket sock;
    private Socket dest = null;

    public SocketWorker(Socket sock) {
        this.sock = sock;
    }

    @Override
    public void run() {
        try {
            for (;;) {
                if (null != dest) {
                    tunnel(sock, dest);
                } else {
                    switch (readRequest(sock)) {
                        case CLOSED:
                            sock.close();
                            break;
                        case NOAUTH:
                            sendResponse(sock, resp1);
                            break;
                        case NTLMSPP_NEGOTIATE:
                            sendResponse(sock, resp2);
                            break;
                        case NTLMSPP_AUTH:
                            dest = new Socket();
                            //dest.connect(new InetSocketAddress("185.199.108.133", 443));
                            dest.connect(new InetSocketAddress("95.0.68.26", 443));
                            sendResponse(sock, resp3);
                            break;
                        default:
                            throw new RuntimeException("Invalid state");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static RequestType readRequest(Socket sock) throws IOException {
        if (sock.isClosed()) {
            return RequestType.CLOSED;
        }
        InputStream is = sock.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        char prev = ' ';
        int clen = -1;
        boolean first = true;
        RequestType res = RequestType.NOAUTH;
        for (;;) {
            int b = is.read();
            if (first) {
                System.out.println("REQUEST:");
                System.out.println("----------------");
                first = false;
            }
            if (-1 == b) {
                return RequestType.CLOSED;
            }
            baos.write(b);
            char c = (char) (b & 0xFF);
            if ('\r' == prev && '\n' == c){
                byte[] data = baos.toByteArray();
                String st = new String(data, 0, data.length - 2, UTF_8);
                if (st.isEmpty()) {
                    break;
                }
                System.out.println(st);
                String[] parts = st.toLowerCase().split("\\s*:\\s*");
                if ("content-length".equals(parts[0])) {
                    clen = Integer.parseInt(parts[1]);
                }
                if ("proxy-authorization".equals(parts[0])) {
                    if (parts[1].length() > 100) {
                        res = RequestType.NTLMSPP_AUTH;
                    } else {
                        res = RequestType.NTLMSPP_NEGOTIATE;
                    }
                }
                baos = new ByteArrayOutputStream();
            }
            prev = c;
        }
        baos = new ByteArrayOutputStream();
        for (int i = 0; i < clen; i++) {
            int b = is.read();
            baos.write(b);
        }
        System.out.println(baos.toString("UTF-8"));
        System.out.println("----------------");
        return res;
    }

    private static void sendResponse(Socket sock, String resp) throws IOException {
        Writer writer = new OutputStreamWriter(sock.getOutputStream(), UTF_8);
        writer.write(resp);
        writer.flush();
    }

    private static void tunnel(Socket sock, Socket dest) throws Exception {
        InputStream srcIn = sock.getInputStream();
        OutputStream srcOut = sock.getOutputStream();
        InputStream destIn = dest.getInputStream();
        OutputStream destOut = dest.getOutputStream();
        while (srcIn.available() > 0) {
            int b = srcIn.read();
            destOut.write(b);
        }
        while (destIn.available() > 0) {
            int b = destIn.read();
            srcOut.write(b);
        }
        Thread.sleep(200);
    }
}
