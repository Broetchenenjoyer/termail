import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class TLS {

    SSLSocket socket;
    BufferedReader reader;
    BufferedWriter writer;

    public void connect(String host, int port) throws IOException {
        this.socket = (SSLSocket)SSLSocketFactory.getDefault().createSocket(host, port);
        SSLParameters p = socket.getSSLParameters();
        p.setEndpointIdentificationAlgorithm("HTTPS"); // Hostname-Check
        this.socket.setSSLParameters(p);
        this.socket.startHandshake();

        // ISO_8858_1: 1 Byte = 1 Zeichen, damit Literal-Längen (in Bytes) stimmen
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1));

        String greeting = reader.readLine();
        if (!expectedGreeting(greeting)) {
            throw new IOException("Unerwartete Begrüßung: " + greeting);
        }
    }

    public boolean expectedGreeting(String greeting) {
        return true;
    }

    public static String getDomain(String adr) {
        if (adr.contains("@")) {
            return adr.split("@")[1];
        }
        return adr.toLowerCase();
    }
}
