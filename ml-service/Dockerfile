# ──────────────────────────────────────────────────────────────
# Stage 1: builder — training + artifact packaging
# ──────────────────────────────────────────────────────────────
FROM python:3.12-slim AS builder

WORKDIR /build

# Install build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
        git ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

# Copy and install ALL dependencies
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy training code
COPY train.py ./

# Build model artifact with metadata
ARG MODEL_VERSION=dev-unknown
ARG GIT_SHA=unknown
ARG BUILD_DATE=unknown

ENV MODEL_VERSION=${MODEL_VERSION}
ENV GIT_SHA=${GIT_SHA}
ENV BUILD_DATE=${BUILD_DATE}

# train.py saves failure_model.joblib + metadata.json to /build/models/
RUN python train.py

# ──────────────────────────────────────────────────────────────
# Stage 2: runtime — inference only
# ──────────────────────────────────────────────────────────────
FROM python:3.12-slim AS runtime

WORKDIR /app

# Copy installed Python packages from builder
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copy only runtime application code
COPY app/ ./app/

# Copy trained model + metadata (NO train.py, NO data/)
COPY --from=builder /build/models/ ./models/

ENV MODEL_VERSION=${MODEL_VERSION:-dev-unknown}

EXPOSE 8000

HEALTHCHECK --interval=15s --timeout=5s --retries=8 --start-period=20s \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
