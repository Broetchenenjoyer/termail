#!/bin/sh
# termail-update.sh: lädt das neueste Release von Termail und installiert/aktualisiert es
set -eu

REPO="Broetchenenjoyer/termail"
APP="termail"
SHARE="$HOME/.local/share/$APP"
BIN="$HOME/.local/bin"
DESKTOP="$HOME/.local/share/applications"

mkdir -p "$SHARE" "$BIN" "$DESKTOP"

# Neuestes Release abfragen
JSON=$(curl -fsSL "https://api.github.com/repos/$REPO/releases/latest")
TAG=$(printf '%s' "$JSON" | grep -m1 '"tag_name"' | sed 's/.*: *"\(.*\)".*/\1/')
URL=$(printf '%s' "$JSON" | grep '"browser_download_url"' | grep -m1 '\.jar"' | sed 's/.*: *"\(.*\)".*/\1/')

[ -n "$TAG" ] || { echo "Couldn't find a release." >&2; exit 1; }
[ -n "$URL" ] || { echo "No .jar-Asset in release $TAG." >&2; exit 1; }

# Update nötig?
if [ -f "$SHARE/VERSION" ] && [ "$(cat "$SHARE/VERSION")" = "$TAG" ]; then
    echo "allready up to date ($TAG)."
    exit 0
fi

echo "installing $TAG ..."
curl -fsSL -o "$SHARE/$APP.jar.tmp" "$URL"
mv "$SHARE/$APP.jar.tmp" "$SHARE/$APP.jar"
printf '%s' "$TAG" > "$SHARE/VERSION"

# Starter im PATH
cat > "$BIN/$APP" <<EOF
#!/bin/sh
exec java -jar "$SHARE/$APP.jar" "\$@"
EOF
chmod +x "$BIN/$APP"

# Launcher-Eintrag (TUI -> Terminal=true)
cat > "$DESKTOP/$APP.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=Termail
Comment=Terminal-Mailclient
Exec=$APP
Terminal=true
Categories=Network;Email;
EOF

command -v update-desktop-database >/dev/null && update-desktop-database "$DESKTOP" || true

echo "ready: $TAG"
