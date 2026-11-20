#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

VENV="$HOME/.venvs/cctvall-cloudcam"
if [ ! -x "$VENV/bin/python" ]; then
  echo "CloudCam belum dipasang. Jalankan setup-android-tv.sh terlebih dahulu."
  exit 1
fi

if [ "$#" -lt 5 ] || [ "$#" -gt 5 ]; then
  echo "Cara pakai:"
  echo "  ~/bin/add-rtsp-camera NAMA IP PORT USERNAME PATH_RTSP"
  echo "Contoh:"
  echo "  ~/bin/add-rtsp-camera tuya_kamera1 192.168.1.80 554 admin /stream1"
  exit 2
fi

NAME="$1"
HOST="$2"
PORT="$3"
USERNAME="$4"
RTSP_PATH="$5"

if [[ ! "$NAME" =~ ^[A-Za-z0-9_-]{1,64}$ ]]; then
  echo "Nama harus 1-64 karakter: huruf, angka, garis bawah, atau tanda hubung."
  exit 2
fi
if [[ "$HOST" == *"/"* || "$HOST" == *"://"* || "$HOST" == *"@"* || -z "$HOST" ]]; then
  echo "IP/host kamera tidak valid."
  exit 2
fi
if [[ ! "$PORT" =~ ^[0-9]{1,5}$ ]] || (( PORT < 1 || PORT > 65535 )); then
  echo "Port harus berupa angka 1-65535."
  exit 2
fi
if [[ -z "$RTSP_PATH" ]]; then
  echo "Path RTSP tidak boleh kosong."
  exit 2
fi
[[ "$RTSP_PATH" == /* ]] || RTSP_PATH="/$RTSP_PATH"

read -r -s -p "Password ONVIF kamera (input tersembunyi): " PASSWORD
echo
if [ -z "$PASSWORD" ]; then
  echo "Password tidak boleh kosong."
  exit 2
fi

export CLOUDCAM_HOME="$HOME/.config/cloudcam"
export CLOUDCAM_PLATFORM=hikconnect
export CCTV_RTSP_NAME="$NAME"
export CCTV_RTSP_HOST="$HOST"
export CCTV_RTSP_PORT="$PORT"
export CCTV_RTSP_USER="$USERNAME"
export CCTV_RTSP_PATH="$RTSP_PATH"
export CCTV_RTSP_PASSWORD="$PASSWORD"

"$VENV/bin/python" - <<'PY'
import os
from urllib.parse import quote
from cloudcam.sources.rtsp import store

name = os.environ["CCTV_RTSP_NAME"]
host = os.environ["CCTV_RTSP_HOST"]
port = os.environ["CCTV_RTSP_PORT"]
username = quote(os.environ["CCTV_RTSP_USER"], safe="")
password = quote(os.environ["CCTV_RTSP_PASSWORD"], safe="")
path = os.environ["CCTV_RTSP_PATH"]

url = f"rtsp://{username}:{password}@{host}:{port}{path}"
camera = store.add(name, url)
print("Kamera berhasil disimpan ke penyimpanan CloudCam.")
print(f"Nama: {camera.name}")
print(f"Path WebRTC setelah server dimulai ulang: rtsp/{camera.name}")
print("Password tidak ditampilkan. File penyimpanan CloudCam dibuat dengan izin privat.")
PY

unset CCTV_RTSP_PASSWORD PASSWORD
echo
echo "Berikutnya: hentikan CloudCam dengan Ctrl+C, lalu jalankan ~/bin/start-cloudcam-server lagi."
echo "Setelah server hidup, isi Path WebRTC di HTML dengan: rtsp/$NAME"
