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
    private final SMTP smtp;

    private boolean inTextBox = false;
    private String mailBody = "";

    public Tui(Config cfg, SMTP smtp) throws Exception {
        this.cfg = cfg;
        this.smtp = smtp;

        // needed so the terminals colors and so on can be used
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

            accounts = new ComboBox<>(cfg.getEmails()) {
                @Override
                public Interactable.Result handleKeyStroke(KeyStroke key) {
                    return Tui.noArrowFocus(super.handleKeyStroke(key));
                }
            };
            accounts.addListener((selectedIndex, previousSelection, byUser) -> this.selectEmailAdr());
            Border accountsBox = accounts.withBorder(Borders.singleLine());
            accountsBox.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));

            mailDirs = new ComboBox<>(cfg.getIMAP(accounts.getSelectedItem()).getFolders().toArray(new String[0])) {
                @Override
                public Interactable.Result handleKeyStroke(KeyStroke key) {
                    return Tui.noArrowFocus(super.handleKeyStroke(key));
                }
            };
            mailDirs.setSelectedItem(cfg.getCredentials(accounts.getSelectedItem()).STARTDIR);
            mailDirs.addListener((selectedIndex, previusSelection, byUser) -> this.selectMailDir());
            Border mailDirsBox = mailDirs.withBorder(Borders.singleLine());
            mailDirsBox.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));

            list = new MailList(cfg.getIMAP(accounts.getSelectedItem()).getMails(mailDirs.getSelectedItem(), 0, 32), 32);
            list.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill, LinearLayout.GrowPolicy.CanGrow));
            list.setOnSelect(m -> readMail(m));
            list.setPreferredSize(new TerminalSize(Math.max(20, screen.getTerminalSize().getColumns() / 4), 0));

            accountsBox.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
            mailDirsBox.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

            Separator hSepLeft = new Separator(Direction.HORIZONTAL);
            hSepLeft.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
            Separator hSepRight = new Separator(Direction.HORIZONTAL);
            hSepRight.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

            // Links: Account / Linie / Liste
            Panel left = new Panel(new LinearLayout(Direction.VERTICAL));
            left.addComponent(accountsBox);
            left.addComponent(hSepLeft);
            left.addComponent(list);
            left.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

            // Rechts: Ordner / Linie / Detail
            Panel right = new Panel(new LinearLayout(Direction.VERTICAL));
            right.addComponent(mailDirsBox);
            right.addComponent(hSepRight);
            right.addComponent(detail);
            right.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill, LinearLayout.GrowPolicy.CanGrow));

            Separator vSep = new Separator(Direction.VERTICAL);
            vSep.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

            Panel content = new Panel(new LinearLayout(Direction.HORIZONTAL));
            content.addComponent(new EmptySpace(new TerminalSize(1, 0)));
            content.addComponent(left);
            content.addComponent(new EmptySpace(new TerminalSize(1, 0)));
            content.addComponent(vSep);
            content.addComponent(new EmptySpace(new TerminalSize(1, 0)));
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
            for (String s : cfg.getIMAP(accounts.getSelectedItem()).getFolders()) {
                mailDirs.addItem(s);
            }
            mailDirs.setSelectedItem(cfg.getCredentials(accounts.getSelectedItem()).STARTDIR);
            list.setMails(cfg.getIMAP(accounts.getSelectedItem()).getMails(mailDirs.getSelectedItem(), 0, 32));
            detail.setText("");
            mailBody = "";
        } catch (Exception e) {
            detail.setText("Fehler: " + e.getMessage());
        }
    }



    private void selectMailDir() {
        try {
            list.setMails(cfg.getIMAP(accounts.getSelectedItem()).getMails(mailDirs.getSelectedItem(), 0, 32));
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
        list.setPreferredSize(new TerminalSize(Math.max(20, newSize.getColumns() / 4), 0));
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
