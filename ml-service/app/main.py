from fastapi import FastAPI

from app.model import FailureModel
from app.schemas import RiskScoreResponse, TelemetryPayload

app = FastAPI(
    title="Power Asset Failure Prediction Service",
    description="ML service for predicting electrical grid equipment failure probability",
    version="1.0.0",
)

model = FailureModel()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


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
