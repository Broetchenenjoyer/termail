import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Tui extends WindowListenerAdapter {
    private static SimpleTheme theme;
    private ComboBox<String> accounts;
    private ComboBox<String> mailDirs;
    private MailList list;
    private TextBox detail;
    private final Config cfg;
    private final IMAP imap;
    private final SMTP smtp;

    private boolean inTextBox = false;
    private String mailBody = "";

    public Tui(Config cfg, IMAP imap, SMTP smtp) throws Exception {
        this.cfg = cfg;
        this.imap = imap;
        this.smtp = smtp;

        //needed so the terminals theme should be used
        theme = SimpleTheme.makeTheme(false, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT, TextColor.ANSI.BLACK, TextColor.ANSI.WHITE, TextColor.ANSI.DEFAULT);

        Screen screen = new TerminalScreen(new DefaultTerminalFactory().createTerminal());
        screen.startScreen();

        try {
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.DEFAULT));
            gui.setTheme(theme);

            BasicWindow window = new BasicWindow("Termail");
            window.setHints(List.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_POST_RENDERING));
            window.addWindowListener(this);
            window.setEnableDirectionBasedMovements(false);

            detail = new TextBox(new TerminalSize(40, 10), TextBox.Style.MULTI_LINE) {
                @Override
                public Interactable.Result handleKeyStroke(KeyStroke key) {
                    return Tui.noArrowFocus(super.handleKeyStroke(key));
                }
            };
            detail.setReadOnly(true);
            detail.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill, LinearLayout.GrowPolicy.CanGrow));
            detail.setEnabled(inTextBox);

            Panel right = new Panel();
            right.addComponent(detail);
            right.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill, LinearLayout.GrowPolicy.CanGrow));

            accounts = new ComboBox<>(cfg.getEmails()) {
                @Override
                public Interactable.Result handleKeyStroke(KeyStroke key) {
                    return Tui.noArrowFocus(super.handleKeyStroke(key));
                }
            };
            accounts.addListener((selectedIndex, previousSelection, byUser) -> this.selectEmailAdr());
            Border accountsBox = accounts.withBorder(Borders.singleLine());
            accountsBox.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

            mailDirs = new ComboBox<>(imap.getFolders(cfg.getCredentials(accounts.getSelectedItem())).toArray(new String[0])) {
                @Override
                public Interactable.Result handleKeyStroke(KeyStroke key) {
                    return Tui.noArrowFocus(super.handleKeyStroke(key));
                }
            };
            mailDirs.setSelectedItem(cfg.getCredentials(accounts.getSelectedItem()).STARTDIR);
            mailDirs.addListener((selectedIndex, previusSelection, byUser) -> this.selectMailDir());
            Border mailDirsBox = mailDirs.withBorder(Borders.singleLine());
            mailDirsBox.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

            list = new MailList(imap.getMails(cfg.getCredentials(accounts.getSelectedItem()), mailDirs.getSelectedItem(), 0, 32), 32);
            list.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill, LinearLayout.GrowPolicy.CanGrow));
            list.setOnSelect(m -> readMail(m));

            Panel left = new Panel(new LinearLayout(Direction.VERTICAL));
            left.addComponent(accountsBox);
            left.addComponent(mailDirsBox);
            left.addComponent(new EmptySpace(new TerminalSize(0, 1)));
            left.addComponent(list);
            left.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

            Separator sep = new Separator(Direction.VERTICAL);
            sep.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

            Panel content = new Panel(new LinearLayout(Direction.HORIZONTAL));
            content.addComponent(new EmptySpace(new TerminalSize(1, 0))); // Padding left
            content.addComponent(left);
            content.addComponent(new EmptySpace(new TerminalSize(1, 0))); // distance to line
            content.addComponent(sep);
            content.addComponent(new EmptySpace(new TerminalSize(1, 0))); // distance after the line
            content.addComponent(right);

            window.setComponent(content);
            gui.addWindowAndWait(window);
        } finally {
            screen.stopScreen();
        }
    }



    private void selectEmailAdr() {
        try {
            mailDirs.clearItems();
            for (String s : imap.getFolders(cfg.getCredentials(accounts.getSelectedItem()))) {
                mailDirs.addItem(s);
            }
            mailDirs.setSelectedItem(cfg.getCredentials(accounts.getSelectedItem()).STARTDIR);
            list.setMails(imap.getMails(cfg.getCredentials(accounts.getSelectedItem()), mailDirs.getSelectedItem(), 0, 32));
            detail.setText("");
            mailBody = "";
        } catch (Exception e) {
            detail.setText("Fehler: " + e.getMessage());
        }
    }



    private void selectMailDir() {
        try {
            list.setMails(imap.getMails(cfg.getCredentials(accounts.getSelectedItem()), mailDirs.getSelectedItem(), 0, 32));
            detail.setText("");
            mailBody = "";
        } catch (Exception e) {
            detail.setText("Fehler: " + e.getMessage());
        }
    }



    private void readMail(EMail m) {
        inTextBox = true;
        mailBody = m.getMailBodyFormated(accounts.getSelectedItem());
        wrapText(mailBody);
        detail.setEnabled(inTextBox);
        detail.takeFocus();
        detail.setCaretPosition(0, 0);
    }



    private void wrapText(String text) {
        int w = detail.getSize().getColumns() - 1;
        List<String> lines = TerminalTextUtils.getWordWrappedText(w, text.split("\n", -2));
        detail.setText(String.join("\n", lines));
    }



    public static Interactable.Result noArrowFocus(Interactable.Result r) {
        return switch (r) {
            case MOVE_FOCUS_UP, MOVE_FOCUS_DOWN, MOVE_FOCUS_LEFT, MOVE_FOCUS_RIGHT -> Interactable.Result.HANDLED;
            default -> r;
        };
    }



    @Override
    public void onResized(Window window, TerminalSize oldSize, TerminalSize newSize) {
        if (!detail.getText().isBlank() && this.cfg.wantsTextWrapped()) {
            wrapText(mailBody);
        }
    }



    @Override
    public void onUnhandledInput(Window basePane, KeyStroke key, AtomicBoolean handled) {
        switch (key.getKeyType()) {
        case KeyType.Escape -> {
            if (inTextBox) {
                list.takeFocus();
                inTextBox = false;
                detail.setEnabled(inTextBox);
            } else {
                basePane.close();
            }
            handled.set(true);
        }
        }
    }
}
