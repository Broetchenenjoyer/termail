import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import java.util.List;
import java.util.function.Consumer;

public class MailList extends AbstractInteractableComponent<MailList> {
    private static final int ITEM_HEIGHT = 4; // 3 Zeilen + 1 Leerzeile

    private List<EMail> mails;
    private final int width;
    private int selected = 0;
    private int scrollTop = 0; // Index der ersten sichtbaren Mail
    private int visible = 1;   // Anzahl sichtbarer Mails
    private Consumer<EMail> onSelect = m -> {};

    public MailList(List<EMail> mails, int width) {
        this.mails = mails;
        this.width = width;
    }



    public EMail getSelected() {
        return mails.isEmpty() ? null : mails.get(selected);
    }



    public void setOnSelect(Consumer<EMail> onSelect) {
        this.onSelect = onSelect;
    }



    private void move(int index) {
        if (mails.isEmpty())
            return;
        selected = Math.max(0, Math.min(mails.size() - 1, index));
        invalidate();
    }



    public void setMails(List<EMail> mails) {
        this.mails = mails;
        this.selected = 0;
        this.scrollTop = 0;
        invalidate();
    }



    @Override
    public Result handleKeyStroke(KeyStroke key) {
        switch (key.getKeyType()) {
        case ArrowDown -> {
            move(selected + 1);
            return Result.HANDLED;
        }
        case ArrowUp -> {
            move(selected - 1);
            return Result.HANDLED;
        }
        case PageDown -> {
            move(selected + visible);
            return Result.HANDLED;
        }
        case PageUp -> {
            move(selected - visible);
            return Result.HANDLED;
        }
        case Home -> {
            move(0);
            return Result.HANDLED;
        }
        case End -> {
            move(mails.size() - 1);
            return Result.HANDLED;
        }
        case Enter -> {
            if (getSelected() != null)
                onSelect.accept(getSelected());
            return Result.HANDLED;
        }
        default -> {
            return super.handleKeyStroke(key);
        }
        }
    }



    private static String fit(String s, int w) {
        if (w <= 0)
            return "";
        if (s.length() > w)
            s = s.substring(0, w - 1) + "…";
        return String.format("%-" + w + "s", s);
    }



    @Override
    protected InteractableRenderer<MailList> createDefaultRenderer() {
        return new InteractableRenderer<>() {
            @Override
            public TerminalPosition getCursorLocation(MailList c) { return null; }



            @Override
            public TerminalSize getPreferredSize(MailList c) {
                return new TerminalSize(width, ITEM_HEIGHT);
            }



            @Override
            public void drawComponent(TextGUIGraphics g, MailList c) {
                int w = g.getSize().getColumns();
                int h = g.getSize().getRows();

                if (w <= 0 || h <= 0)
                    return;

                visible = Math.max(1, (h + 1) / ITEM_HEIGHT);

                if (selected < scrollTop)
                    scrollTop = selected;
                if (selected >= scrollTop + visible)
                    scrollTop = selected - visible + 1;

                g.fill(' ');
                for (int i = 0; i < visible && scrollTop + i < mails.size(); i++) {
                    EMail m = mails.get(scrollTop + i);
                    boolean sel = scrollTop + i == selected;
                    int y = i * ITEM_HEIGHT;

                    if (sel)
                        g.enableModifiers(SGR.REVERSE);
                    g.putString(0, y, fit(m.DATE, w));
                    g.putString(0, y + 1, fit(m.SUBJECT, w));
                    g.putString(0, y + 2, fit(m.SENDER, w));
                    if (sel)
                        g.disableModifiers(SGR.REVERSE);
                }
            }
        };
    }
}
