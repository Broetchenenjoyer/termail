import java.io.IOException;
import java.nio.file.Path;

public class Main {
    private static SMTP smtp;
    private static Config cfg;

    private static boolean ui = true;

    public static void main(String[] args) {
        try {
            cfg = new Config(System.getProperty("user.home") + "/.config/termail/config.lua");

            cfg.loginAll();
            smtp = new SMTP(cfg);

            runArgs(args);

            if (ui) {
                new Tui(cfg, smtp);
            }

            cfg.logoutAll();

        } catch (Exception e) {
            System.err.println("Idk what happend. I am sowy: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private static void runArgs(String[] args) {
        String sender = "", reciever = "", subject = "", message = "";
        boolean send = false;
        Path[] attachments = null;
        try {
            for (String s : args) {
                String[] split = s.split("=");
                switch (split[0].trim()) {
                case "noui" -> {
                    ui = false;
                }

                case "help" -> {
                    help();
                    return;
                }

                case "send" -> {
                    send = true;
                }

                case "sender" -> {
                    sender = split[1];
                }

                case "reciever" -> {
                    reciever = split[1];
                }

                case "subject" -> {
                    subject = split[1];
                }

                case "message" -> {
                    message = split[1];
                }

                case "attachments" -> {
                    String[] paths = s.split("={")[1].substring(0, s.split("={")[1].length() - 1).split(",");
                    attachments = new Path[paths.length];

                    for (int i = 0; i < paths.length; i++) {
                        attachments[i] = Path.of(paths[i]);
                    }
                }

                default -> {
                        System.err.println("unknown argument: " + s);
                        System.out.println("To get an overview of arguments run \"help\"");
                }
                }
            }
        } catch (IndexOutOfBoundsException ioobe) {
            System.err.println("Invalid arguments: " + ioobe.getMessage());
            ioobe.printStackTrace();
        }

        try {
            if (send && !sender.isBlank() && !reciever.isBlank() && !subject.isBlank() && !message.isEmpty()) {
                if (attachments == null) {
                    smtp.sendMail(sender, reciever, subject, message.split("\n"));
                } else {
                    smtp.sendMail(sender, reciever, subject, message.split("\n"), attachments);
                }
                System.out.println("Mail sent");
            }
        } catch (IOException ioe) {
            System.err.println("Could not send Mail: " + ioe.getMessage());
        }
    }

    public static void help() {
        System.out.println("""
            To send an E-Mail:
            use 'send'
            and specify: 'sender="" reciever="" subject="" message="" and optionally attachment={"", ""}'

            To run the programm without ui:
            use 'noui'
            """);
    }
}
