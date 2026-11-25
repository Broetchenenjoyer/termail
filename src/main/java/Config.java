import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.*;

public class Config {

    public static final int HIDE = 0, DELETE = 1, MOVE = 2;
    private static final Globals globals = JsePlatform.standardGlobals();

    private final File config;

    private final HashMap<String, Credentials> credentials = new HashMap<>();
    private final HashMap<String, MailServer> mailServers = new HashMap<>();
    private final HashMap<String, IMAP> imapClients = new HashMap<>();

    private final List<String> spamDomains = new ArrayList<>();
    private final List<String> spamMails = new ArrayList<>();
    private final List<Pattern> spamRegex = new ArrayList<>();
    private int spamAction = HIDE;
    private String spamDestination = "Spam";

    private boolean linewrap = false;
    private int maxLinebreakCount = 1;
    private String texteditor = "native";

    public Config(String path) throws IOException {
        config = new File(path);

        if (!config.exists()) {
            generateConfig(config.toPath());
        }

        globals.loadfile(config.getPath()).call();
        LuaValue cfg = globals.get("config");

        LuaValue mails = cfg.get("mails");
        if (mails.istable()) {
            for (int i = 1; i <= mails.length(); i++) {
                LuaValue login = mails.get(i);
                Credentials credential = new Credentials(login.get("mail").tojstring(), login.get("pw").tojstring(), login.get("starting_folder").tojstring());
                credentials.put(login.get("mail").tojstring(), credential);
            }
        }

        LuaValue servers = cfg.get("servers");
        if (servers.istable()) {
            for (int i = 1; i <= servers.length(); i++) {
                LuaValue server = servers.get(i);
                LuaValue smtp = server.get("smtp");
                LuaValue imap = server.get("imap");
                mailServers.put(server.get("mail_domain").tojstring(), new MailServer(smtp.get("adr").tojstring(), smtp.get("port").toint(), imap.get("adr").tojstring(), imap.get("port").toint()));
            }
        }

        LuaValue spamfilter = cfg.get("spamfilter");
        if (spamfilter.istable()) {
            mails = spamfilter.get("mails");
            if (mails.istable()) {
                for (int i = 1; i <= mails.length(); i++) {
                    spamMails.add(mails.get(i).tojstring());
                }
            }
            LuaValue domains = spamfilter.get("domains");
            if (domains.istable()) {
                for (int i = 1; i <= domains.length(); i++) {
                    spamDomains.add(domains.get(i).tojstring());
                }
            }
            LuaValue subjects = spamfilter.get("subjects");
            if (subjects.istable()) {
                LuaValue regexs = subjects.get("regex");
                if (regexs.istable()) {
                    for (int i = 1; i <= regexs.length(); i++) {
                        spamRegex.add(Pattern.compile(regexs.get(i).tojstring()));
                    }
                }
            }

            if (!spamfilter.get("action").isnil()) {
                switch (spamfilter.get("action").tojstring().split(" ")[0]) {
                case "move" -> {
                    spamAction = MOVE;
                    spamDestination = spamfilter.get("action").tojstring().split(" ")[1];
                }
                case "hide" -> {
                    spamAction = HIDE;
                }
                case "delete" -> {
                    spamAction = DELETE;
                }
                }
            }
        }

        this.texteditor = cfg.get("texteditor").optjstring("native");

        LuaValue text = cfg.get("text");
        if (text.istable()) {
            maxLinebreakCount = text.get("max_linebreak_count").optint(1);
            linewrap = text.get("linewrap").optboolean(false);
        }

        for (Map.Entry<String, Credentials> set : credentials.entrySet()) {
            imapClients.put(set.getKey(), new IMAP(set.getValue(), this));
        }
    }



    public boolean isSpamSubject(String subject) {
        for (Pattern p : spamRegex) {
            if (p.matcher(subject).find()) {
                return true;
            }
        }
        return false;
    }



    public boolean isSpamDomain(String sender) {
        for (String s : spamDomains) {
            if (sender.toLowerCase().contains(s.toLowerCase().strip()) && !s.isBlank())
                return true;
        }
        return false;
    }



    public boolean isSpamAddress(String sender) {
        for (String s : spamMails) {
            if (sender.toLowerCase().contains(s.toLowerCase().strip()) && !s.isBlank())
                return true;
        }
        return false;
    }



    public Credentials getCredentials(String mail) {
        return credentials.get(mail);
    }



    public MailServer getMailServer(String adr) {
        return mailServers.get(adr);
    }



    public String[] getEmails() {
        return credentials.keySet().toArray(new String[0]);
    }



    public int getMaxLineBreakCount() {
        return maxLinebreakCount;
    }



    public boolean wantsTextWrapped() {
        return linewrap;
    }



    public int getSpamAction() {
        return spamAction;
    }



    public String getSpamDestination() {
        return spamDestination;
    }



    public String getTextEditor() {
        return texteditor;
    }



    public IMAP getIMAP(String mail) {
        return imapClients.get(mail);
    }



    public void loginAll() throws IOException {
        for (IMAP imap : imapClients.values()) {
            imap.login();
        }
    }



    public void logoutAll() throws IOException {
        for (IMAP imap : imapClients.values()) {
            imap.logout();
        }
    }



    public void generateConfig(Path path) throws IOException {
        Files.createDirectory(path.getParent());
        Files.createFile(path);
        Files.writeString(path, """
                -- this is an automatically generated config, this config will generate, if you run termail without config.
                -- to be able to use termail you will have to specify atleast one E-Mail and the correspondig password, otherwise it cant do its job and throw an Exception, and I am to lazy to add login.

                -- █▄▄                                   ▄▄▄                           ▄▄      ▄▄▄▄▄▄▄▄         ▄▄▄▄▄▄▄▄         ▄▄▄▄▄▄▄▄         ▄▄▄▄▄▄▄▄         ▄▄▄▄▄   
                -- ▐███▄▄               ▄▄     ▄▄▄      ░████▌             ▄▄▄▄▄     ▄▄▄▀▓▌   ▄▄██▓▀▀▀██▓▄▄    ▄▄██▓▀▀▀██▓▄▄    ▄▄██▓▀▀▀██▓▄▄    ▄▄██▓▀▀▀██▓▄▄   ▄▄█▀█▓▀██▄▄
                -- ▐█▄███             ▄▄█▌     ▐███     █▓▄█▓█▌            ██████   ▐█████▌ ▄▄███▄████▓▄██▀  ▄▄███▄████▓▄██▀  ▄▄███▄████▓▄██▀  ▄▄███▄████▓▄██▀  █████▓▀▀█▓█▀
                -- ▐█▄█▓█▌           ▒▓██▌     ▐█▄██   ▐██████▓▌           ███▄██▌   ▐▓▄▓█▌▐█▄█▓█▀    ▀▀█   ▐█▄█▓█▀    ▀▀█   ▐█▄█▓█▀    ▀▀█   ▐█▄█▓█▀    ▀▀█    █▓█▄█▄    ▀ 
                -- ▐▐███▓▌          ░▓███▌      ▐██▓▌  ░███████▌           ▐█████▌   ▐█▓███ ▐███████▒▌       ▐███████▒▌       ▐███████▒▌       ▐███████▒▌        ▀▀██▓▄▄    
                -- ▐█████▌          ▐████▌      ▐███▌ ▐███▓ ▀███▌          ▐█████████▓████▒▐█████▌          ▐█████▌          ▐█████▌          ▐█████▌              ▀█████░░▄
                --  ▐█▄▄██▄▄         ▐███▓▄    ▄▓██▌  ▐██████████▌          ▀▀▀▀▀▀▀▀▀▀████░ ▐█▄▄██▄▄         ▐█▄▄██▄▄         ▐█▄▄██▄▄         ▐█▄▄██▄▄         ▐█▄    ▓█▄▄▌
                --   ▀▐█████▓████▓░█ ▀█████▓███████  ▐██████▀██▓██▌        ▐████▌    ▐▓████  ▀▐█████▓████▓░█  ▀▐█████▓████▓░█  ▀▐█████▓████▓░█  ▀▐█████▓████▓░█▓██████████▓▌
                --      ▀▀▀▀██████▓    ▀▀▀▀████▀▀    ▀▓█▓    ▐█████        ▐███▓██▄▄▄████▀▀     ▀▀▀▀██████▓      ▀▀▀▀██████▓      ▀▀▀▀██████▓      ▀▀▀▀██████▓  ▀▀▀▓█████▀▀ 
                --                                            █▀▀▀▀         ▀▀▀▀▀▀▀▀▀▀▀▀▀                                                                                   

                config = {
                    mails = {
                        -- you can set your E-Mails and corresponding passwords directly, write a function, or do what ever to retieve them.
                        -- if you want to be save retrieve them from a passwordmanager and dont put them here in PLAIN TEXT!!!
                        {
                            mail = "",
                            pw = "",
                            starting_folder = "INBOX"
                        },
                    },

                    servers = {
                        {
                            -- set the addresses and ports to what your E-Mail provider specified. Also cativate the necessary settings for your account, or the connection can't be established.
                            mail_domain = "",
                            smtp = { adr = "", port = 465 },
                            imap = { adr = "", port = 993 },
                        },
                    },

                    -- everyone has some prefferences to how the text should look
                    text = {
                        max_linebreak_count = 1,
                        textwrap = false,
                    },
                    
                    -- this is a spamfilter, it filters spam, because spam exists
                    spamfilter = {
                        -- you can specify a List of E-Mails, to be flagged as Spam, so you don't have to look at them
                        mails = {},
                        -- you can specify entire domains, if you don't like that domain specifically
                        domains = {},
                        subjects = {
                            -- you can also specify regex, for the E-Mail Headers
                            regex = {
                                "(?i)Black Week",
                                "(?i)Black Friday",
                            },
                        },

                        -- Decide what to do with E-Mails that got flaged as SPAM action are:
                        -- "hide" (doesn't show up in your tui and is the default), 
                        -- "delete" (deletes the mails directly),
                        -- "move [destination]" (moves the mails to your specified destination)
                        action = "hide",
                        
                        -- I hate spam, have multiple E-Mails and dont want to make a spamfilter for all of them, so here we are
                    },
                }
                """);
    }
}
