from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

BASE_DIR = Path(__file__).resolve().parent
DATA_PATH = BASE_DIR / "data" / "telemetry_training.csv"
MODEL_PATH = BASE_DIR / "models" / "failure_model.joblib"

FEATURE_COLUMNS = [
    "temperature_c",
    "load_pct",
    "vibration_mm_s",
    "voltage_kv",
    "humidity_pct",
    "age_years",
    "failure_count_12m",
]
TARGET_COLUMN = "failure_within_30d"


def generate_synthetic_dataset(path: Path, n_samples: int = 3000) -> None:
    rng = np.random.default_rng(seed=42)

    temperature = rng.normal(70, 15, size=n_samples).clip(20, 140)
    load = rng.normal(65, 20, size=n_samples).clip(10, 150)
    vibration = rng.normal(3.5, 1.8, size=n_samples).clip(0.1, 15)
    voltage = rng.normal(115, 10, size=n_samples).clip(80, 150)
    humidity = rng.normal(50, 20, size=n_samples).clip(5, 100)
    age = rng.normal(18, 8, size=n_samples).clip(0, 60)
    failures = rng.poisson(0.8, size=n_samples).clip(0, 10)

    logits = (
        -9.0
        + 0.06 * temperature
        + 0.03 * load
        + 0.35 * vibration
        - 0.01 * voltage
        + 0.012 * humidity
        + 0.05 * age
        + 0.45 * failures
    )
    probs = 1 / (1 + np.exp(-logits))
    y = rng.binomial(1, probs)

    df = pd.DataFrame(
        {
            "temperature_c": temperature,
            "load_pct": load,
            "vibration_mm_s": vibration,
            "voltage_kv": voltage,
            "humidity_pct": humidity,
            "age_years": age,
            "failure_count_12m": failures,
            "failure_within_30d": y,
        }
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(path, index=False)


def train() -> None:
    if not DATA_PATH.exists():
        generate_synthetic_dataset(DATA_PATH)

    df = pd.read_csv(DATA_PATH)
    x = df[FEATURE_COLUMNS]
    y = df[TARGET_COLUMN]

    x_train, x_test, y_train, y_test = train_test_split(
        x, y, test_size=0.2, random_state=42, stratify=y
    )

    preprocessor = ColumnTransformer(
        transformers=[("num", StandardScaler(), FEATURE_COLUMNS)]
    )
    clf = LogisticRegression(max_iter=1000)

    pipeline = Pipeline([
        ("preprocessor", preprocessor),
        ("classifier", clf),
    ])
    pipeline.fit(x_train, y_train)

    acc = pipeline.score(x_test, y_test)
    print(f"Validation accuracy: {acc:.4f}")

    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, MODEL_PATH)
    print(f"Model saved to {MODEL_PATH}")


if __name__ == "__main__":
    train()
