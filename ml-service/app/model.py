from pathlib import Path
import joblib
import numpy as np
import pandas as pd

MODEL_PATH = Path(__file__).resolve().parents[1] / "models" / "failure_model.joblib"


FEATURE_COLUMNS = [
    "temperature_c",
    "load_pct",
    "vibration_mm_s",
    "voltage_kv",
    "humidity_pct",
    "age_years",
    "failure_count_12m",
]


class FailureModel:
    def __init__(self) -> None:
        if not MODEL_PATH.exists():
            raise FileNotFoundError(
                f"Model file was not found: {MODEL_PATH}. Run train.py first."
            )
        self.pipeline = joblib.load(MODEL_PATH)

    def predict_failure_probability(self, features: list[float]) -> float:
        # Convert list to DataFrame with correct column names
        features_df = pd.DataFrame([features], columns=FEATURE_COLUMNS)
        probability = self.pipeline.predict_proba(features_df)[0][1]
        return float(probability)
