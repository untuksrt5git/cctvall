#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

VENV="$HOME/.venvs/cctvall-cloudcam"
if [ ! -x "$VENV/bin/cloudcam" ]; then
  echo "CloudCam belum dipasang. Jalankan setup-android-tv.sh terlebih dahulu."
  exit 1
fi
source "$VENV/bin/activate"

mkdir -p "$HOME/.config/cloudcam"
export CLOUDCAM_HOME="$HOME/.config/cloudcam"
export CLOUDCAM_PLATFORM=hikconnect

echo "== Login CloudCam Hik-Connect =="
read -r -p "Username / User ID / Email Hik-Connect: " CLOUDCAM_EMAIL
read -r -s -p "Password Hik-Connect: " CLOUDCAM_PASSWORD
echo
export CLOUDCAM_EMAIL CLOUDCAM_PASSWORD

echo
echo "Buat akun baca untuk perangkat viewer (STB/HP)."
read -r -p "Username viewer: " READ_USER
read -r -s -p "Password viewer (min. 12 karakter): " READ_PASS
echo

if [ "${#READ_USER}" -lt 3 ] || [ "${#READ_PASS}" -lt 12 ]; then
  echo "Username minimal 3 karakter dan password minimal 12 karakter."
  unset CLOUDCAM_PASSWORD
  exit 1
fi

echo
MEDIAMTX_BIN="$(command -v mediamtx || true)"
if [ -z "$MEDIAMTX_BIN" ] || [ ! -x "$MEDIAMTX_BIN" ]; then
  echo "MediaMTX belum terpasang. Jalankan: pkg install mediamtx"
  exit 1
fi

RTP_PORT="${CCTV_RTP_PORT:-18000}"
if [[ ! "$RTP_PORT" =~ ^[0-9]{1,5}$ ]] || (( RTP_PORT < 1024 || RTP_PORT > 65534 )); then
  echo "Port RTP tidak valid: $RTP_PORT (gunakan angka 1024-65534)."
  unset CLOUDCAM_PASSWORD READ_PASS
  exit 1
fi

echo "Menjalankan CloudCam + MediaMTX: $MEDIAMTX_BIN"
echo "RTSP TCP: 8554 | RTSP RTP/RTCP UDP: $RTP_PORT/$((RTP_PORT + 1)) | WebRTC: 8889/8189"
echo "Jangan membuka port ini langsung ke internet. Akses jarak jauh disiapkan melalui tunnel/VPN aman."
echo

cloudcam serve \
  --listen 0.0.0.0 \
  --read-user "$READ_USER" \
  --read-pass "$READ_PASS" \
  --mediamtx "$MEDIAMTX_BIN" \
  --rtp-port "$RTP_PORT"

unset CLOUDCAM_PASSWORD READ_PASS
