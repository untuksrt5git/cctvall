#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

echo "== CCTV All-in-One: persiapan CloudCam untuk Android TV =="
echo "Gunakan Termux resmi dari F-Droid atau GitHub, bukan Play Store lama."

pkg update -y
pkg upgrade -y
# NumPy is installed from Termux; requests and PyCryptodome are installed with pip.
pkg install -y python python-numpy ffmpeg git curl clang make pkg-config mediamtx

VENV="$HOME/.venvs/cctvall-cloudcam"
mkdir -p "$HOME/.venvs"
if [ ! -x "$VENV/bin/python" ]; then
  # Gunakan NumPy native Termux; dependensi Python lainnya dipasang melalui pip.
  python -m venv --system-site-packages "$VENV"
fi

"$VENV/bin/python" -m pip install --upgrade pip setuptools wheel
"$VENV/bin/python" -m pip install requests pycryptodome
"$VENV/bin/python" -m pip install --no-deps --upgrade \
  "git+https://github.com/Bahrombekk/cloud-cam-viewer.git@3d2f057ae138ef7320258caf2a8d3764b5e8114f"

"$VENV/bin/python" -c "import numpy, requests; from Crypto.Cipher import AES; print('Python dependencies OK:', numpy.__version__, requests.__version__)"
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
echo "MediaMTX dipasang melalui paket Termux (mediamtx). Periksa dengan: command -v mediamtx"
