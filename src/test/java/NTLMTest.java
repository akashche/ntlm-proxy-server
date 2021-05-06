import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NTLMTest {

    static int port = 8080;

    @BeforeClass
    public static void startServer() throws IOException {
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", String.valueOf(8080));
        System.setProperty("http.maxRedirects", String.valueOf(3));
        Authenticator.setDefault(new Auth());
        Server server = new Server(port);
        server.start();
    }

    @Test
    public void test1() throws Exception {
        //URL url = new URL("https://raw.githubusercontent.com/akashche/timestamp-server/master/README.md");
        URL url = new URL("https://hs01.kep.tr/squirrelmail/esign/pttkep-eimza.php");
        //URL url = new URL("http://127.0.0.1:" + port);
//        URL url = new URL("https://localhost:8443/");
        /*
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setDoOutput(false);
        conn.connect();
        readResponse(conn);
        conn.disconnect();
         */

        System.out.println("EXIT");
        Thread.sleep(10000000);
    }

    private static void writeToOutput(HttpURLConnection conn, String msg) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            Writer writer = new OutputStreamWriter(os, UTF_8);
            writer.write(msg);
            writer.flush();
        }
    }

    private static void readResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        System.out.println("RESPONSE:");
        System.out.println("----------------");
        for (int i = 0; i < conn.getHeaderFields().size(); i++) {
            String key = conn.getHeaderFieldKey(i);
            String val = conn.getHeaderField(i);
            if (null == key) {
                System.out.println(val);
            } else {
                System.out.println(key + ": " + val);
            }
        }
        InputStream is = responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();
        if (null != is) {
            StringBuilder sb = new StringBuilder();
            Reader re = new InputStreamReader(is, UTF_8);
            char[] buf = new char[1024];
            int read = 0;
            while (-1 != (read = re.read(buf))) {
                sb.append(buf, 0, read);
            }
            System.out.println(sb.toString());
        }
        System.out.println("----------------");
    }

    private static class Auth extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            System.out.println(getRequestingURL());
            return new PasswordAuthentication("test", "secret".toCharArray());
        }
    }
}
