#!/usr/bin/env python3
"""Authenticated HTTP camera catalog for the CloudCam Hik-Connect bridge."""
from __future__ import annotations

import hmac
import json
import logging
import os
import re
import signal
import socket
import subprocess
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import quote

from cloudcam import CloudCam

LOG = logging.getLogger("hikconnect-bridge")
SERIAL_RE = re.compile(r"^[A-Za-z0-9_-]{1,64}$")


def required(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


BRIDGE_TOKEN = required("CLOUDCAM_BRIDGE_TOKEN")
if len(BRIDGE_TOKEN) < 24:
    raise RuntimeError("CLOUDCAM_BRIDGE_TOKEN must be at least 24 characters")

STREAM_USER = required("CLOUDCAM_STREAM_USER")
STREAM_PASSWORD = required("CLOUDCAM_STREAM_PASSWORD")
STREAM_HOST = required("CLOUDCAM_STREAM_HOST")
API_PORT = int(os.environ.get("CLOUDCAM_BRIDGE_PORT", "8765"))

if any(ch in STREAM_HOST for ch in "/?#@ "):
    raise RuntimeError("CLOUDCAM_STREAM_HOST must be a hostname or IP address only")

cloud = CloudCam()
cloud.login()
cloud.start_token_refresh(interval=900)
stream_process: subprocess.Popen | None = None
stream_lock = __import__("threading").Lock()

SERVE_COMMAND = [
    sys.executable, "-m", "cloudcam", "serve",
    "--listen", "0.0.0.0",
    "--rtsp-port", "8554",
    "--hls-port", "8888",
    "--read-user", STREAM_USER,
    "--read-pass", STREAM_PASSWORD,
]


def ensure_stream_service() -> None:
    """Start MediaMTX only after the account actually returns a camera."""
    global stream_process
    with stream_lock:
        if stream_process is not None and stream_process.poll() is None:
            return
        stream_process = subprocess.Popen(SERVE_COMMAND)
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            if stream_process.poll() is not None:
                raise RuntimeError("CloudCam stream service exited during startup")
            try:
                with socket.create_connection(("127.0.0.1", 8554), timeout=0.4):
                    return
            except OSError:
                time.sleep(0.25)
        if stream_process.poll() is None:
            stream_process.terminate()
        raise RuntimeError("CloudCam RTSP service did not become ready")


def camera_payload() -> dict:
    cameras = cloud.cameras()
    items = []
    safe_user = quote(STREAM_USER, safe="")
    safe_password = quote(STREAM_PASSWORD, safe="")
    for camera in cameras:
        serial = str(camera.serial)
        channel = int(camera.channel)
        if not SERIAL_RE.fullmatch(serial) or channel < 1:
            continue
        stream_path = f"cam/{serial}_{channel}"
        items.append({
            "id": f"{serial}_{channel}",
            "serial": serial,
            "channel": channel,
            "name": str(camera.name or f"{serial} CH{channel}"),
            "rtsp_url": (
                f"rtsp://{safe_user}:{safe_password}@{STREAM_HOST}:8554/{stream_path}"
            ),
        })

    if items:
        ensure_stream_service()

    return {
        "status": "ok",
        "count": len(items),
        "stream_service": "ready" if items else "waiting_for_cameras",
        "message": (
            "Camera list is empty. The shared devices may not be exposed by this account/API."
            if not items else ""
        ),
        "cameras": items,
    }


class Handler(BaseHTTPRequestHandler):
    server_version = "CCTV-HikConnect-Bridge/1.1"

    def log_message(self, fmt: str, *args) -> None:
        # Never log headers, credentials, or stream URLs.
        LOG.info("%s - %s", self.client_address[0], fmt % args)

    def send_json(self, status: int, payload: dict) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def authorised(self) -> bool:
        supplied = self.headers.get("X-Bridge-Token", "")
        return hmac.compare_digest(supplied, BRIDGE_TOKEN)

    def do_GET(self) -> None:
        if self.path == "/health":
            running = stream_process is not None and stream_process.poll() is None
            self.send_json(200, {
                "status": "ok",
                "stream_service": "ready" if running else "not_started",
            })
            return
        if self.path != "/v1/cameras":
            self.send_json(404, {"error": "not_found"})
            return
        if not self.authorised():
            self.send_json(401, {"error": "unauthorised"})
            return
        try:
            self.send_json(200, camera_payload())
        except Exception as exc:
            LOG.exception("Could not list Hik-Connect cameras")
            self.send_json(503, {
                "error": "camera_list_unavailable",
                "detail": type(exc).__name__,
            })

    def do_POST(self) -> None:
        if self.path != "/v1/refresh":
            self.send_json(404, {"error": "not_found"})
            return
        if not self.authorised():
            self.send_json(401, {"error": "unauthorised"})
            return
        try:
            self.send_json(200, camera_payload())
        except Exception as exc:
            LOG.exception("Could not refresh Hik-Connect camera list")
            self.send_json(503, {
                "error": "camera_list_unavailable",
                "detail": type(exc).__name__,
            })


def shutdown(_signum=None, _frame=None) -> None:
    LOG.info("Stopping Hik-Connect bridge")
    if stream_process is not None and stream_process.poll() is None:
        stream_process.terminate()
        try:
            stream_process.wait(timeout=8)
        except subprocess.TimeoutExpired:
            stream_process.kill()
    cloud.close()
    raise SystemExit(0)


if __name__ == "__main__":
    import threading

    logging.basicConfig(
        level=os.environ.get("LOG_LEVEL", "INFO").upper(),
        format="%(asctime)s %(levelname)s %(message)s",
    )
    signal.signal(signal.SIGTERM, shutdown)
    signal.signal(signal.SIGINT, shutdown)
    server = ThreadingHTTPServer(("0.0.0.0", API_PORT), Handler)
    LOG.info("Hik-Connect bridge API listening on port %s", API_PORT)
    try:
        server.serve_forever(poll_interval=0.5)
    finally:
        server.server_close()
        shutdown()
