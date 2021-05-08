

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SocketWorker implements Runnable {
    private static final String resp1 = "HTTP/1.1 407 Proxy Authentication Required\r\n" +
            "Content-Length: 0\r\n" +
            "Proxy-Authenticate: NEGOTIATE\r\n" +
            "Proxy-Authenticate: NTLM\r\n" +
            "Proxy-Authenticate: BASIC realm=\"test1\"\r\n" +
            "Proxy-Connection: close\r\n" +
            "Connection: close\r\n" +
            "Content-Length: 7394\r\n" +
            "\r\n" +
            genRest(7394);
    private static final String resp2 = "HTTP/1.1 407 Proxy Authentication Required\r\n" +
            "Content-Length: 0\r\n" +
            "Proxy-Authenticate: NTLM TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==\r\n" +
            "Proxy-Connection: Keep-Alive\r\n" +
            "Connection: Keep-Alive\r\n" +
            "Content-Length: 7411\r\n" +
            "\r\n" +
            genRest(7411);
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
            outer: for (;;) {
                if (null != dest) {
                    tunnel(sock, dest);
                } else {
                    Map<String, String> headers = readRequest(sock);
                    switch (detectRequestType(headers)) {
                        case CLOSED:
                            sock.close();
                            break outer;
                        case NOAUTH:
                            sendResponse(sock, resp1);
                            break outer;
                        case NTLMSPP_NEGOTIATE:
                            sendResponse(sock, resp2);
                            break;
                        case NTLMSPP_AUTH:
                            String line = headers.get("");
                            String hostPort = line.split(" ")[1];
                            String[] parts = hostPort.split(":");
                            String host = parts[0];
                            int port = Integer.parseInt(parts[1]);
                            dest = new Socket();
                            dest.connect(new InetSocketAddress(host, port));
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
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> readRequest(Socket sock) throws IOException {
        if (sock.isClosed()) {
            return Collections.emptyMap();
        }
        InputStream is = sock.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        char prev = ' ';
        int clen = -1;
        boolean first = true;
        boolean firstLine = true;
        Map<String, String> res = new LinkedHashMap<>();
        for (;;) {
            int b = is.read();
            if (first) {
                System.out.println("REQUEST:");
                System.out.println("----------------");
                first = false;
            }
            if (-1 == b) {
                return Collections.emptyMap();
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
                if (firstLine) {
                    res.put("", st);
                    firstLine = false;
                } else {
                    String[] parts = st.split("\\s*:\\s*");
                    String key = parts[0].toLowerCase();
                    String val = parts[1];
                    res.put(key, val);
                    if ("content-length".equals(key)) {
                        clen = Integer.parseInt(val);
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

    private RequestType detectRequestType(Map<String, String> headers) {
        String pa = headers.get("proxy-authorization");
        if (null == pa) {
            return RequestType.NOAUTH;
        } else if (pa.length() > 100) {
            return RequestType.NTLMSPP_AUTH;
        } else {
            return RequestType.NTLMSPP_NEGOTIATE;
        }
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
        long outbound = 0;
        long inbound = 0;
        byte[] buf = new byte[4096];
        while (srcIn.available() > 0) {
            int read = srcIn.read(buf);
            outbound += read;
            destOut.write(buf, 0, read);
        }
        while (destIn.available() > 0) {
            int read = destIn.read(buf);
            inbound += read;
            srcOut.write(buf, 0, read);
        }
        System.out.println("Tunneling, outbound bytes: [" + outbound + "], inbound bytes: [" + inbound + "]");
        Thread.sleep(1000);
    }

    private static String genRest(int length) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < length; i++) {
            sb.append(i % 10);
        }
        return sb.toString();
    }

}
