# ML Failure Prediction Service (FastAPI + scikit-learn)

## Project structure

```text
ml-service/
├── app/
│   ├── main.py            # FastAPI API
│   ├── model.py           # Model loading/inference
│   └── schemas.py         # Request/response schemas
├── data/
│   └── telemetry_training.csv
├── models/
│   └── failure_model.joblib
├── train.py               # Training pipeline
├── requirements.txt
└── Dockerfile
```

## Train model

```bash
python train.py
```

## Run API locally

```bash
uvicorn app.main:app --reload --port 8000
```

## Predict endpoint

`POST /v1/predict`

Example request:

```json
{
  "asset_id": "TR-101",
  "temperature_c": 92.0,
  "load_pct": 87.0,
  "vibration_mm_s": 6.1,
  "voltage_kv": 110.0,
  "humidity_pct": 63.0,
  "age_years": 21.0,
  "failure_count_12m": 2
}
```

## Java integration

Use Spring `WebClient`/`RestTemplate` to call `POST http://ml-service:8000/v1/predict`.
The response contains `failure_probability`, `risk_score`, and `risk_level`, which can be merged into your existing risk workflow.
