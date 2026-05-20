from pathlib import Path
import joblib

MODEL_PATH = Path(__file__).resolve().parents[1] / "models" / "failure_model.joblib"


class FailureModel:
    def __init__(self) -> None:
        if not MODEL_PATH.exists():
            raise FileNotFoundError(
                f"Model file was not found: {MODEL_PATH}. Run train.py first."
            )
        self.pipeline = joblib.load(MODEL_PATH)

    def predict_failure_probability(self, features: list[float]) -> float:
        probability = self.pipeline.predict_proba([features])[0][1]
        return float(probability)
