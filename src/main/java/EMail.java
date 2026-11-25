import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EMail {
    private static final Pattern UID_PATTERN = Pattern.compile("\\bUID (\\d+)");
    private static final Pattern TIMEZONE = Pattern.compile("\\s*[+-]\\d{4}\\b.*$");

    public final long UID;
    public final String SENDER, SUBJECT, DATE, DIR;
    private final boolean spam;

    private final IMAP imap;
    private final Config cfg;

    public EMail(String dir, long uid, String sender, String subject, String date, Config cfg, IMAP imap) {
        this.UID = uid;
        this.SENDER = sender.substring(Math.max(sender.lastIndexOf('<') + 1, 0), sender.lastIndexOf('>') < 0 ? sender.length() : sender.lastIndexOf('>'));
        this.SUBJECT = subject;
        this.DATE = date;
        this.DIR = dir;
        this.spam = cfg.isSpamDomain(imap.getDomain(sender)) || cfg.isSpamAddress(sender) || cfg.isSpamSubject(subject);
        this.imap = imap;
        this.cfg = cfg;
    }



    public boolean isSpam() {
        return spam;
    }



    public String getMailBody(String mailAdr) {
        try {
            try {
                imap.select(DIR);
                return imap.fetchText(UID);
            } finally {
                try {
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            return "Couldn't retrieve E-Mail-Body: " + e.getMessage();
        }
    }



    public String getMailBodyFormated(String mailAdr) {
        return getMailBody(mailAdr).replaceAll("(?:\\r?\\n){" + (cfg.getMaxLineBreakCount() + 1) + ",}", "\n".repeat(cfg.getMaxLineBreakCount()));
    }



    public static List<EMail> parse(String dir, List<String> response, Config cfg, IMAP imap) {
        List<EMail> result = new ArrayList<>();

        for (String element : response) {
            if (!element.startsWith("* "))
                continue;

            int headerStart = element.indexOf("\r\n");
            int tailStart = element.lastIndexOf("\r\n");
            if (headerStart < 0)
                continue;

            String firstLine = element.substring(0, headerStart);
            String tail = element.substring(tailStart + 2);
            long uid = findUid(firstLine);
            if (uid < 0)
                uid = findUid(tail);
            if (uid < 0)
                continue;

            String headers = element.substring(headerStart + 2, tailStart + 2).replaceAll("\r\n[ \t]+", " ");

            String from = "", subject = "", date = "";
            for (String line : headers.split("\r\n")) {
                int colon = line.indexOf(':');
                if (colon < 0)
                    continue;

                String name = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();

                if (name.equalsIgnoreCase("From"))
                    from = value;
                else if (name.equalsIgnoreCase("Subject"))
                    subject = IMAP.decode(value);
                else if (name.equalsIgnoreCase("Date"))
                    date = TIMEZONE.matcher(value).replaceFirst("");
            }

            result.add(new EMail(dir, uid, from, subject, date, cfg, imap));
        }
        return result;
    }



    private static long findUid(String s) {
        Matcher m = UID_PATTERN.matcher(s);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }
}
