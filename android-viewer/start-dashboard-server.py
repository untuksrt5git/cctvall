#!/usr/bin/env python3
"""LAN-only dashboard server with a small authenticated RTSP-camera registration API.

Run with the CloudCam virtual-environment Python so the private RTSP store module is available.
The one-time API token is printed to the terminal and is never written to disk.
"""
from __future__ import annotations

import hmac
import ipaddress
import json
import os
import re
import secrets
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parent
HOME = Path.home()
os.environ.setdefault("CLOUDCAM_HOME", str(HOME / ".config" / "cloudcam"))
os.environ.setdefault("CLOUDCAM_PLATFORM", "hikconnect")
API_TOKEN = secrets.token_urlsafe(24)
MAX_BODY = 8192
NAME_RE = re.compile(r"^[A-Za-z0-9_-]{1,64}$")
PORT_RE = re.compile(r"^[0-9]{1,5}$")
PATH_RE = re.compile(r"^/[A-Za-z0-9._~!$&'()*+,;=:@%/?-]{1,512}$")


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def log_message(self, _format, *_args):
        # Do not log requests; credentials must never appear in terminal logs.
        return

    def _json(self, status: int, payload: dict):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/api/health":
            self._json(200, {"ok": True, "service": "cctv-dashboard"})
            return
        if self.path == "/":
            self.path = "/index.html"
        super().do_GET()

    def do_POST(self):
        if self.path != "/api/rtsp/add":
            self._json(404, {"ok": False, "error": "Endpoint tidak ditemukan."})
            return

        try:
            remote = ipaddress.ip_address(self.client_address[0])
            if not (remote.is_private or remote.is_loopback):
                self._json(403, {"ok": False, "error": "Hanya jaringan lokal yang diizinkan."})
                return
        except ValueError:
            self._json(403, {"ok": False, "error": "Alamat klien tidak valid."})
            return

        host_header = self.headers.get("Host", "")
        origin = self.headers.get("Origin")
        if origin:
            from urllib.parse import urlsplit
            if urlsplit(origin).netloc.lower() != host_header.lower():
                self._json(403, {"ok": False, "error": "Origin tidak cocok dengan dashboard."})
                return

        supplied = self.headers.get("X-CCTV-Token", "")
        if not hmac.compare_digest(supplied, API_TOKEN):
            self._json(401, {"ok": False, "error": "Kode pengelola tidak benar."})
            return

        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            length = 0
        if length < 2 or length > MAX_BODY:
            self._json(413, {"ok": False, "error": "Ukuran data tidak valid."})
            return
        try:
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            self._json(400, {"ok": False, "error": "Data formulir tidak valid."})
            return

        name = str(payload.get("name", "")).strip()
        host = str(payload.get("host", "")).strip()
        port_text = str(payload.get("port", "554")).strip()
        username = str(payload.get("username", "")).strip()
        password = str(payload.get("password", ""))
        rtsp_path = str(payload.get("path", "")).strip()

        if not NAME_RE.fullmatch(name):
            self._json(400, {"ok": False, "error": "Nama harus 1–64 karakter: huruf, angka, _ atau -."})
            return
        try:
            address = ipaddress.ip_address(host)
            if not address.is_private and not address.is_loopback:
                raise ValueError("outside local LAN")
            if address.version != 4:
                raise ValueError("IPv4 required")
        except ValueError:
            self._json(400, {"ok": False, "error": "Isi IP kamera IPv4 di jaringan lokal, misalnya 192.168.1.80."})
            return
        if not PORT_RE.fullmatch(port_text) or not 1 <= int(port_text) <= 65535:
            self._json(400, {"ok": False, "error": "Port RTSP harus 1–65535."})
            return
        if not username or len(username) > 128 or any(c in username for c in "\r\n"):
            self._json(400, {"ok": False, "error": "Username ONVIF/RTSP tidak valid."})
            return
        if not password or len(password) > 256 or any(c in password for c in "\r\n"):
            self._json(400, {"ok": False, "error": "Password wajib diisi (maks. 256 karakter, tanpa baris baru)."})
            return
        if not PATH_RE.fullmatch(rtsp_path) or ".." in rtsp_path or "#" in rtsp_path:
            self._json(400, {"ok": False, "error": "Path harus diawali / dan hanya berisi karakter URL yang aman."})
            return

        safe_user = quote(username, safe="")
        safe_password = quote(password, safe="")
        rtsp_url = f"rtsp://{safe_user}:{safe_password}@{host}:{int(port_text)}{rtsp_path}"

        try:
            from cloudcam.sources.rtsp import store
            camera = store.add(name, rtsp_url)
        except Exception as exc:
            # Error text might contain a URL; never return its raw text to the browser.
            self._json(409, {"ok": False, "error": "CloudCam tidak dapat menyimpan kamera. Pastikan nama belum digunakan dan CloudCam sudah terpasang di virtual environment."})
            return

        self._json(201, {
            "ok": True,
            "name": camera.name,
            "path": f"rtsp/{camera.name}",
            "restart_required": True,
            "message": "Kamera tersimpan. Hentikan CloudCam dengan Ctrl+C lalu jalankan kembali agar path aktif."
        })


def main():
    import argparse
    parser = argparse.ArgumentParser(description="CCTV dashboard + protected local RTSP registration API")
    parser.add_argument("--bind", default="0.0.0.0", help="Alamat bind (default: 0.0.0.0 untuk LAN)")
    parser.add_argument("--port", type=int, default=8090)
    args = parser.parse_args()
    if not 1024 <= args.port <= 65535:
        parser.error("port harus 1024–65535")

    server = ThreadingHTTPServer((args.bind, args.port), Handler)
    server.daemon_threads = True
    print(f"Dashboard: http://127.0.0.1:{args.port}/")
    print(f"Untuk HP lain: http://IP-HP-SERVER:{args.port}/")
    print("Kode pengelola web (berlaku sampai server ini dihentikan):")
    print(API_TOKEN)
    print("Gunakan hanya pada Wi-Fi/LAN tepercaya. Jangan teruskan port 8090 ke internet.")
    print("Tekan Ctrl+C untuk menghentikan dashboard server.")
    try:
        server.serve_forever(poll_interval=0.5)
    except KeyboardInterrupt:
        print("\nDashboard server dihentikan.")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
