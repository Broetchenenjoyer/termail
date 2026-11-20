public class MailServer {
    public final String IMAP, SMTP;
    public final int IMAPPORT, SMTPPORT;
    public MailServer(String smtp, int smtpport, String imap, int imapport){
        this.IMAP = imap;
        this.IMAPPORT = imapport;
        this.SMTP = smtp;
        this.SMTPPORT = smtpport;
    }
}
