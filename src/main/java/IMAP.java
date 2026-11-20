import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.*;

public class IMAP extends TLS {
    private static final Pattern LITERAL = Pattern.compile("\\{(\\d+)\\}$");
    private static final Pattern EXISTS = Pattern.compile("^\\* (\\d+) EXISTS");

    private int tagCounter = 0;

    private final Config cfg;

    public IMAP(Config cfg) {
        this.cfg = cfg;
    }

    public List<String> getFolders(Credentials credential) throws IOException {
        ArrayList<String> result = new ArrayList<>();

        connect(cfg.getMailServer(getDomain(credential.MAIL)).IMAP, cfg.getMailServer(getDomain(credential.MAIL)).IMAPPORT);
        login(credential.MAIL, credential.PW);

        for (String s : command("LIST \"\" \"*\"")) {
            if (s.charAt(0) == '*')
                result.add(s.split("\"/\"")[1].strip());
        }

        logout();
        return result;
    }



    public List<EMail> getMails(Credentials credential, String serverDir, int from, int to) throws IOException {
        ArrayList<EMail> mails = new ArrayList<>();

        connect(cfg.getMailServer(getDomain(credential.MAIL)).IMAP, cfg.getMailServer(getDomain(credential.MAIL)).IMAPPORT);
        login(credential.MAIL, credential.PW);
        int count = select(serverDir);

        if (from < count) {
            for (EMail m : EMail.parse(serverDir, fetchHeaders(count - from, Math.max(1, count - to)), cfg, this)) {
                if (!m.isSpam()) {
                    mails.add(m);
                }
            }
        }

        logout();
        return mails.reversed();
    }



    public void deleteMails(Credentials credential, List<EMail> mails) throws IOException {
        connect(cfg.getMailServer(getDomain(credential.MAIL)).IMAP, cfg.getMailServer(getDomain(credential.MAIL)).IMAPPORT);
        login(credential.MAIL, credential.PW);

        for (EMail m : mails) {
            delete(m.UID);
        }

        logout();
    }



    public void moveMails(Credentials credential, List<EMail> mails, String target) throws IOException {
        connect(cfg.getMailServer(getDomain(credential.MAIL)).IMAP, cfg.getMailServer(getDomain(credential.MAIL)).IMAPPORT);
        login(credential.MAIL, credential.PW);

        for (EMail m : mails) {
            move(m.UID, target);
        }

        logout();
    }



    public String fetchText(long uid) throws IOException {
        for (String l : fetchMessage(uid)) {
            int s = l.indexOf("\r\n");
            int e = l.lastIndexOf(')');
            if (l.startsWith("* ") && s > 0 && e > s)
                return MimeParser.text(l.substring(s + 2, e));
        }
        return "";
    }



    public List<String> command(String cmd) throws IOException {
        String tag = String.format("A%03d", ++tagCounter);
        writer.write(tag + " " + cmd + "\r\n");
        writer.flush();

        List<String> response = new ArrayList<>();
        while (true) {
            String first = reader.readLine();
            if (first == null)
                throw new IOException("Verbindung geschlossen");

            StringBuilder sb = new StringBuilder(first);
            Matcher m;
            while ((m = LITERAL.matcher(sb)).find()) {
                int n = Integer.parseInt(m.group(1));
                char[] buf = new char[n];
                int off = 0;
                while (off < n) {
                    int r = reader.read(buf, off, n - off);
                    if (r < 0)
                        throw new IOException("Verbindung während Literal geschlossen");
                    off += r;
                }
                sb.append("\r\n").append(buf);
                String rest = reader.readLine();
                if (rest == null)
                    throw new IOException("Verbindung geschlossen");
                sb.append(rest);
            }

            String line = sb.toString();
            response.add(line);

            if (line.startsWith(tag + " ")) {
                if (!line.startsWith(tag + " OK")) {
                    throw new IOException("IMAP-Fehler: " + line);
                }
                return response;
            }
        }
    }



    public static String decode(String s) {
        Matcher m = Pattern.compile("=\\?([^?]+)\\?([BbQq])\\?([^?]*)\\?=").matcher(s);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            String between = s.substring(last, m.start());
            if (!between.isBlank())
                out.append(between); // Leerraum zwischen Encoded-Words entfällt
            try {
                Charset cs = Charset.forName(m.group(1));
                byte[] data;
                if (m.group(2).equalsIgnoreCase("B")) {
                    data = Base64.getDecoder().decode(m.group(3));
                } else {
                    data = decodeQ(m.group(3));
                }
                out.append(new String(data, cs));
            } catch (Exception e) {
                out.append(m.group()); // im Zweifel unverändert lassen
            }
            last = m.end();
        }
        out.append(s.substring(last));
        return out.toString();
    }



    private static byte[] decodeQ(String s) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_')
                bos.write(' ');
            else if (c == '=' && i + 2 < s.length() + 0 && i + 2 <= s.length() - 1 + 0) {
                bos.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
                i += 2;
            } else
                bos.write(c);
        }
        return bos.toByteArray();
    }



    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }



    public void move(long uid, String target) throws IOException {
        command("UID COPY " + uid + " \"" + target + "\"");
        command("UID STORE " + uid + " +FLAGS (\\Deleted)");
        command("UID EXPUNGE " + uid);
    }



    public void delete(long uid) throws IOException {
        command("UID STORE " + uid + " +FLAGS (\\Deleted)");
        command("UID EXPUNGE " + uid);
    }



    public void login(String user, String pass) throws IOException {
        command("LOGIN " + quote(user) + " " + quote(pass));
    }



    public int select(String mailbox) throws IOException {
        int count = 0;
        for (String l : command("SELECT " + quote(mailbox))) {
            Matcher m = EXISTS.matcher(l);
            if (m.find())
                count = Integer.parseInt(m.group(1));
        }
        return count;
    }



    public List<String> fetchHeaders(int from, int to) throws IOException {
        return command("FETCH " + from + ":" + to + " (UID FLAGS BODY.PEEK[HEADER.FIELDS (FROM SUBJECT DATE)])");
    }



    public List<String> fetchMessage(long uid) throws IOException {
        return command("UID FETCH " + uid + " BODY.PEEK[]");
    }



    public void logout() throws IOException {
        command("LOGOUT");
    }



    @Override
    public boolean expectedGreeting(String greeting) {
        return greeting.startsWith("* OK");
    }
}
