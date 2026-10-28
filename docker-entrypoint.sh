#!/bin/sh
set -eu

mkdir -p /data
Xvfb :0 -screen 0 1280x900x24 -ac -nolisten tcp &
xvfb_pid=$!
export DISPLAY=:0

attempt=0
while [ ! -S /tmp/.X11-unix/X0 ] && [ "$attempt" -lt 50 ]; do
    sleep 0.1
    attempt=$((attempt + 1))
done
if [ ! -S /tmp/.X11-unix/X0 ]; then
    echo "Virtual display did not start." >&2
    exit 1
fi

fluxbox >/tmp/fluxbox.log 2>&1 &
wm_pid=$!
x11vnc -display :0 -forever -shared -nopw -rfbport 5900 >/tmp/x11vnc.log 2>&1 &
vnc_pid=$!
websockify --web=/usr/share/novnc 6080 127.0.0.1:5900 >/tmp/websockify.log 2>&1 &
websockify_pid=$!

cleanup() {
    kill "$websockify_pid" "$vnc_pid" "$wm_pid" "$xvfb_pid" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

java -Djava.awt.headless=false \
    -Dlibrary.database=/data/library.db \
    -cp "/app/out:/app/lib/sqlite-jdbc.jar:/app/lib/slf4j-api.jar" \
    app.Main &
app_pid=$!
wait "$app_pid"
