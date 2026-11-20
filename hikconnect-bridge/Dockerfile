FROM python:3.12-slim-bookworm

ARG MEDIAMTX_VERSION=1.20.0

RUN apt-get update \
 && apt-get install -y --no-install-recommends ffmpeg ca-certificates git \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/hikconnect-bridge
RUN pip install --no-cache-dir "cloudcam @ git+https://github.com/Bahrombekk/cloud-cam-viewer.git" \
 && python -c "from cloudcam import mtx_install; mtx_install.install('${MEDIAMTX_VERSION}', bin_dir='/usr/local/bin')" \
 && mediamtx --version

COPY bridge_server.py /opt/hikconnect-bridge/bridge_server.py

RUN useradd -r -u 10001 -m -d /home/bridge bridge \
 && mkdir -p /data \
 && chown -R bridge:bridge /data /opt/hikconnect-bridge
USER bridge
WORKDIR /data

ENV CLOUDCAM_HOME=/data \
    PYTHONUNBUFFERED=1

EXPOSE 8765 8554 8888
VOLUME /data
CMD ["python", "/opt/hikconnect-bridge/bridge_server.py"]
