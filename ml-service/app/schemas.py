from pydantic import BaseModel, Field


class TelemetryPayload(BaseModel):
    asset_id: str = Field(..., description="Power asset identifier")
    temperature_c: float = Field(..., ge=-50, le=200)
    load_pct: float = Field(..., ge=0, le=200)
    vibration_mm_s: float = Field(..., ge=0, le=100)
    voltage_kv: float = Field(..., gt=0, le=1000)
    humidity_pct: float = Field(..., ge=0, le=100)
    age_years: float = Field(..., ge=0, le=100)
    failure_count_12m: int = Field(..., ge=0, le=100)


class RiskScoreResponse(BaseModel):
    asset_id: str
    failure_probability: float = Field(..., ge=0, le=1)
    risk_score: int = Field(..., ge=0, le=100)
    risk_level: str
