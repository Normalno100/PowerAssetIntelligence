from pathlib import Path
import json

import joblib
import numpy as np
import pandas as pd

MODEL_PATH = Path(__file__).resolve().parents[1] / "models" / "failure_model.joblib"
METADATA_PATH = Path(__file__).resolve().parents[1] / "models" / "metadata.json"


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
        self.metadata = self._load_metadata()

    @staticmethod
    def _load_metadata() -> dict:
        """Load model metadata from JSON file."""
        if METADATA_PATH.exists():
            return json.loads(METADATA_PATH.read_text())
        return {}

    def predict_failure_probability(self, features: list[float]) -> float:
        # Convert list to DataFrame with correct column names
        features_df = pd.DataFrame([features], columns=FEATURE_COLUMNS)
        probability = self.pipeline.predict_proba(features_df)[0][1]
        return float(probability)
