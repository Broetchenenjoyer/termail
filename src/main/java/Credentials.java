import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Credentials {
    public final String MAIL, PW, B64, STARTDIR;

    public Credentials(String mail, String pw, String startDir) {
        this.MAIL = mail;
        this.PW = pw;
        this.B64 = Base64.getEncoder().encodeToString(("\0" + mail  + "\0" + pw).getBytes(StandardCharsets.UTF_8));
        this.STARTDIR = startDir;
    }
}
