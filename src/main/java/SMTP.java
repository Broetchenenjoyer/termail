import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;

public class SMTP extends TLS {

    private final Config cfg;

    public SMTP(Config cfg) {
        this.cfg = cfg;
    }

    public String[] read() throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        String line = "";
        do {
            line = reader.readLine();
            if (line == null)
                throw new IOException("Connection to server closed");
            lines.add(line);
        } while (line.length() > 3 && line.charAt(3) == '-');
        return lines.toArray(new String[0]);
    }



    public void send(String cmd) throws IOException {
        writer.write(cmd + "\r\n");
        writer.flush();
    }



    public void auth(String credentials) throws IOException {
        send("EHLO Sir");

        for (String s : read()) {
            System.out.println(s);
        }

        send("AUTH PLAIN " + credentials);

        for (String s : read()) {
            System.out.println(s);
        }
    }



    public void sendMail(String sender, String recipient, String subject, String[] content) throws IOException {
        connect(cfg.getMailServer(getDomain(sender)).SMTP, cfg.getMailServer(getDomain(sender)).SMTPPORT);
        auth(cfg.getCredentials(sender).B64);

        String encSubject = "=?UTF-8?B?" + java.util.Base64.getEncoder().encodeToString(subject.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "?=";
        send("MAIL FROM:<" + sender + ">");
        expect(250);
        send("RCPT TO:<" + recipient + ">");
        expect(250);
        send("DATA");
        expect(354);

        send("From: " + sender);
        send("To: " + recipient);
        send("Subject: " + encSubject);
        send("Date: " + java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME));
        send("Message-ID: <" + java.util.UUID.randomUUID() + "@" + getDomain(sender) + ">");
        send("MIME-Version: 1.0");

        send("Content-Type: text/plain; charset=UTF-8");
        send("");

        for (String line : content) {
            for (String part : line.split("\\R", -1)) {         // \R = jeder Zeilenumbruch
                send(part.startsWith(".") ? "." + part : part); // Dot-Stuffing
            }
        }
        send(".");
        expect(250);

        send("QUIT");
        expect(221);
    }



    public void sendMail(String sender, String recipient, String subject, String[] content, Path[] attachments) throws IOException {
        connect(cfg.getMailServer(getDomain(sender)).SMTP, cfg.getMailServer(getDomain(sender)).SMTPPORT);
        auth(cfg.getCredentials(sender).B64);

        String encSubject = "=?UTF-8?B?" + java.util.Base64.getEncoder().encodeToString(subject.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "?=";
        send("MAIL FROM:<" + sender + ">");
        expect(250);
        send("RCPT TO:<" + recipient + ">");
        expect(250);
        send("DATA");
        expect(354);

        String boundary = "----=_Part_" + java.util.UUID.randomUUID();

        send("From: " + sender);
        send("To: " + recipient);
        send("Subject: " + encSubject);
        send("Date: " + java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME));
        send("Message-ID: <" + java.util.UUID.randomUUID() + "@web.de>");
        send("MIME-Version: 1.0");
        send("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"");
        send("");

        send("--" + boundary);
        send("Content-Type: text/plain; charset=UTF-8");
        send("Content-Transfer-Encoding: 8bit");
        send("");
        for (String line : content) {
            for (String part : line.split("\\R", -1)) {
                send(part.startsWith(".") ? "." + part : part);
            }
        }

        for (java.nio.file.Path file : attachments) {
            String name = file.getFileName().toString();
            String type = java.nio.file.Files.probeContentType(file);
            if (type == null)
                type = "application/octet-stream";

            String encName = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");

            send("--" + boundary);
            send("Content-Type: " + type);
            send("Content-Disposition: attachment; filename*=UTF-8''" + encName);
            send("Content-Transfer-Encoding: base64");
            send("");

            byte[] data = java.nio.file.Files.readAllBytes(file);
            String b64 = java.util.Base64.getMimeEncoder(76, new byte[] {'\n'}).encodeToString(data);
            for (String l : b64.split("\n")) {
                send(l);
            }
        }

        send("--" + boundary + "--");
        send(".");
        expect(250);

        send("QUIT");
        expect(221);
    }



    private void expect(int code) throws IOException {
        String[] reply = read();
        for (String s : reply)
            System.out.println("S: " + s);
        String last = reply[reply.length - 1];
        if (!last.startsWith(code + " ") && !last.equals(String.valueOf(code))) {
            throw new IOException("Unexpected reply (Expected:" + code + "): " + last);
        }
    }
}
