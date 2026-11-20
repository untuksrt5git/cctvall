"""Embedded CloudCam entry points for CCTV All-in-One.

This module intentionally exposes account/catalog operations only. Upstream
CloudCam's live-stream manager launches child Python and FFmpeg processes,
which are not available in Chaquopy's in-process Android runtime.
"""
from __future__ import annotations

import json
import os
import threading

_RUNTIME_LOCK = threading.RLock()
_RUNTIME = None
_RUNTIME_HOME = None


def runtime_info() -> str:
    """Return embedded Python and dependency information for diagnostics."""
    import sys
    import numpy
    import requests
    from Crypto.Cipher import AES

    # Touch AES so a missing/incompatible crypto extension is detected now.
    AES.new(b"1234567890123456", AES.MODE_ECB)
    return json.dumps({
        "python": sys.version.split()[0],
        "numpy": numpy.__version__,
        "requests": requests.__version__,
        "crypto": "PyCryptodome",
        "abi": os.environ.get("ANDROID_ABI", "Android runtime"),
        "status": "ready",
    })


def login_and_list(account: str, password: str, platform: str = "hikconnect",
                   home: str | None = None) -> str:
    """Log in using CloudCam on-device and return the cloud camera catalogue.

    CloudCam calls this setting ``email`` internally, but sends it to Hik-Connect
    as the API ``account`` field. It may therefore be a username, user ID, or email.

    The password is used for this call only and is never written to settings.
    CloudCam's session token and cache are stored in the app-private directory.
    """
    global _RUNTIME, _RUNTIME_HOME

    account = (account or "").strip()
    if not account or not password:
        raise ValueError("Username, user ID, atau email dan kata sandi wajib diisi.")
    if platform not in ("hikconnect", "ezviz"):
        raise ValueError("Platform CloudCam tidak dikenal.")

    if home:
        os.makedirs(home, mode=0o700, exist_ok=True)
        os.environ["CLOUDCAM_HOME"] = home

    with _RUNTIME_LOCK:
        if _RUNTIME is not None and (_RUNTIME_HOME != home or getattr(_RUNTIME.settings, "email", "") != account):
            try:
                _RUNTIME.close()
            except Exception:
                pass
            _RUNTIME = None

        if _RUNTIME is None:
            from cloudcam import CloudCam, Settings

            settings = Settings.resolve().replace(
                email=account,
                password=password,
                platform=platform,
                warm_cache=False,
                use_gpu=False,
                stream_mode="sub",
            )
            _RUNTIME = CloudCam(settings=settings)
            _RUNTIME_HOME = home

        # connect() resumes the app-private token when one exists; only a
        # first login needs the supplied password.
        _RUNTIME.login()
        cameras = _RUNTIME.cameras()

        result = []
        for camera in cameras:
            result.append({
                "serial": str(camera.serial),
                "channel": int(camera.channel),
                "name": str(camera.name or camera.serial),
                "source": "CloudCam Android",
            })

        return json.dumps({
            "status": "ok",
            "count": len(result),
            "platform": platform,
            "cameras": result,
        }, ensure_ascii=False)


def camera_status() -> str:
    """Read camera catalogue again from the current session."""
    global _RUNTIME
    with _RUNTIME_LOCK:
        if _RUNTIME is None:
            raise RuntimeError("Belum login ke CloudCam.")
        cameras = _RUNTIME.cameras()
        return json.dumps([
            {"serial": str(c.serial), "channel": int(c.channel),
             "name": str(c.name or c.serial)}
            for c in cameras
        ], ensure_ascii=False)


def close_runtime() -> None:
    """Release background CloudCam resources when the app service stops."""
    global _RUNTIME, _RUNTIME_HOME
    with _RUNTIME_LOCK:
        if _RUNTIME is not None:
            _RUNTIME.close()
            _RUNTIME = None
            _RUNTIME_HOME = None
