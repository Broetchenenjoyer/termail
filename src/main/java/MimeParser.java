import java.io.ByteArrayOutputStream;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

public class MimeParser {

    public static String text(String raw) {
        String t = find(raw, "text/plain");
        if (t != null)
            return t;
        t = find(raw, "text/html");
        return t == null ? "" : t.replaceAll("(?is)<(script|style).*?</\\1>", "").replaceAll("<[^>]+>", "").replaceAll("\n{3,}", "\n\n").trim();
    }



    private static String find(String raw, String wanted) {
        int i = raw.indexOf("\r\n\r\n");
        String head = i < 0 ? raw : raw.substring(0, i);
        String body = i < 0 ? "" : raw.substring(i + 4);
        Map<String, String> h = headers(head);

        String ct = h.getOrDefault("content-type", "text/plain");
        String type = ct.split(";")[0].trim().toLowerCase();

        if (type.startsWith("multipart/")) {
            String b = param(ct, "boundary");
            if (b == null)
                return null;
            String[] parts = ("\r\n" + body).split("\r\n--" + Pattern.quote(b));
            for (int k = 1; k < parts.length; k++) { // [0] = Preamble überspringen
                String part = parts[k];
                if (part.startsWith("--"))
                    break; // Ende-Marker
                String r = find(part.replaceFirst("^[ \t]*\r\n", ""), wanted);
                if (r != null)
                    return r;
            }
            return null;
        }
        if (!type.equals(wanted))
            return null;

        String cte = h.getOrDefault("content-transfer-encoding", "7bit").trim().toLowerCase();
        byte[] data;
        switch (cte) {
        case "base64" -> data = Base64.getMimeDecoder().decode(body.trim());
        case "quoted-printable" -> data = qp(body);
        default -> data = body.getBytes(StandardCharsets.ISO_8859_1);
        }
        String cs = param(ct, "charset");
        try {
            return new String(data, cs == null ? StandardCharsets.UTF_8 : Charset.forName(cs));
        } catch (Exception e) {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> headers(String head) {
        Map<String, String> m = new HashMap<>();
        for (String line : head.replaceAll("\r\n[ \t]+", " ").split("\r\n")) {
            int c = line.indexOf(':');
            if (c > 0)
                m.put(line.substring(0, c).trim().toLowerCase(), line.substring(c + 1).trim());
        }
        return m;
    }

    private static String param(String header, String name) {
        Matcher m = Pattern.compile(name + "=\"?([^\";\\s]+)\"?", Pattern.CASE_INSENSITIVE).matcher(header);
        return m.find() ? m.group(1) : null;
    }

    private static byte[] qp(String s) {
        s = s.replace("=\r\n", "");
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=' && i + 2 < s.length()) {
                o.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
                i += 2;
            } else
                o.write(c);
        }
        return o.toByteArray();
    }
}
