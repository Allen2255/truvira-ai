# Dockerfile for the Truvira AI-detector service (Python + FastAPI + a pretrained model).
# One stage is enough: Python code is not compiled into a single file like Java's .jar.

# Official slim Python image (Debian "bookworm" Linux underneath, without extras we don't need).
FROM python:3.13-slim-bookworm

# ENV = environment variables baked into the image:
#   PYTHONDONTWRITEBYTECODE=1 -> don't create .pyc cache files (not useful in a container)
#   PYTHONUNBUFFERED=1        -> print log lines immediately (otherwise they can appear late in "docker logs")
#   PIP_NO_CACHE_DIR=1        -> pip doesn't keep a copy of downloaded files (smaller image)
#   HF_HOME=/models           -> Hugging Face stores the downloaded model HERE; docker-compose mounts a
#                                VOLUME on /models so the 745 MB model downloads only ONCE, ever.
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    HF_HOME=/models

WORKDIR /app

# --- Layer caching: install libraries BEFORE copying our code ---
# PyTorch first, from PyTorch's CPU-only download site: the default PyTorch includes graphics-card
# (CUDA) support worth several GB that a normal cloud server without a GPU can't use.
# Fixed version = the one we tested on the laptop.
RUN pip install torch==2.14.1 --index-url https://download.pytorch.org/whl/cpu

# The other libraries (FastAPI, uvicorn, transformers, pillow...) from requirements.txt.
COPY requirements.txt .
RUN pip install -r requirements.txt

# Our code last: editing app.py only re-runs this small step.
COPY app.py .

# Non-root user (same reason as the backend: least privilege). It must own /models to save the model.
RUN useradd --system --create-home --home-dir /home/truvira truvira \
    && mkdir -p /models && chown -R truvira:truvira /models /app
USER truvira

EXPOSE 8000

# HEALTHCHECK = a small test Docker runs regularly: "does GET /health answer?"
# start-period = 10 minutes of grace time, because on the very FIRST start the model downloads.
# (python is used for the request because the slim image has no curl.)
HEALTHCHECK --interval=30s --timeout=10s --start-period=10m --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health', timeout=5)" || exit 1

# Start the web server. --host 0.0.0.0 = accept connections from OTHER containers
# (the default 127.0.0.1 would only accept connections from inside this same container).
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
