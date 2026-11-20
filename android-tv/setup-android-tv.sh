#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

echo "== CCTV All-in-One: persiapan CloudCam untuk Android TV =="
echo "Pastikan Termux berasal dari F-Droid atau GitHub resmi, bukan Play Store lama."

pkg update -y
pkg install -y python ffmpeg git curl

VENV="$HOME/.venvs/cctvall-cloudcam"
mkdir -p "$HOME/.venvs"
if [ ! -x "$VENV/bin/python" ]; then
  python -m venv "$VENV"
fi

"$VENV/bin/python" -m pip install --upgrade pip
"$VENV/bin/python" -m pip install --upgrade \
  "git+https://github.com/Bahrombekk/cloud-cam-viewer.git@3d2f057ae138ef7320258caf2a8d3764b5e8114f"

"$VENV/bin/cloudcam" --help >/dev/null
ffmpeg -version | head -n 1

mkdir -p "$HOME/bin"
cp "$(dirname "$0")/start-cloudcam-server.sh" "$HOME/bin/start-cloudcam-server"
chmod 700 "$HOME/bin/start-cloudcam-server"

echo
echo "PERSIAPAN SELESAI."
echo "Jalankan server dengan perintah:"
echo "  ~/bin/start-cloudcam-server"
echo
echo "Catatan: pemasangan MediaMTX dan kompatibilitas binari Android akan diuji saat server pertama kali dijalankan."
