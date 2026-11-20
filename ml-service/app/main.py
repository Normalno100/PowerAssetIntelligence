import os

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.model import FailureModel
from app.schemas import RiskScoreResponse, TelemetryPayload

app = FastAPI(
    title="Power Asset Failure Prediction Service",
    description="ML service for predicting electrical grid equipment failure probability",
    version="1.0.0",
)

model = FailureModel()

# ─── API Key authentication ───
ML_API_KEY = os.getenv("ML_API_KEY", "ml-dev-key-change-me")
WHITELIST_PATHS = {"/health", "/docs", "/openapi.json", "/redoc"}


@app.middleware("http")
async def api_key_middleware(request: Request, call_next):
    """Require X-API-Key header for all endpoints except whitelist."""
    if request.url.path in WHITELIST_PATHS:
        return await call_next(request)

    key = request.headers.get("X-API-Key")
    if not key or key != ML_API_KEY:
        return JSONResponse(
            status_code=401,
            content={"detail": "Invalid or missing API key"},
        )

    return await call_next(request)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/model-info")
def model_info() -> dict:
    """Return model metadata (version, hash, build date, etc.)."""
    info = {"status": "loaded"}
    if model.metadata:
        info["metadata"] = model.metadata
    else:
        info["warning"] = "No metadata file found"
    return info


@app.post("/v1/predict", response_model=RiskScoreResponse)
def predict(payload: TelemetryPayload) -> RiskScoreResponse:
    features = [
        payload.temperature_c,
        payload.load_pct,
        payload.vibration_mm_s,
        payload.voltage_kv,
        payload.humidity_pct,
        payload.age_years,
        payload.failure_count_12m,
    ]
    failure_probability = model.predict_failure_probability(features)
    risk_score = round(failure_probability * 100)

    if risk_score >= 80:
        risk_level = "CRITICAL"
    elif risk_score >= 60:
        risk_level = "HIGH"
    elif risk_score >= 30:
        risk_level = "MEDIUM"
    else:
        risk_level = "LOW"

    return RiskScoreResponse(
        asset_id=payload.asset_id,
        failure_probability=failure_probability,
        risk_score=risk_score,
        risk_level=risk_level,
    )
