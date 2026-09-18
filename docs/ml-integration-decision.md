# ADR-NN: ML-сервис не интегрирован в Java-приложение (этап 3)

- **Статус:** Принято
- **Дата:** 2026-09-18
- **Контекст:** Этап 3 — MVP с детерминированным скорингом; ML-сервис существует, но не вызывается из Java-приложения.
- **Решение:** `RuleBasedRiskEngine` остаётся единственной и дефолтной реализацией `CoreRiskScoringPort`. ML-порт и клиент будут добавлены позже как опциональный слой.

---

## Контекст и проблема

ML-сервис (`ml-service/`) развёрнут и доступен по REST (`POST /v1/predict`), однако:

- Java-приложение **никогда не вызывает** `/v1/predict`.
- Единственная реализация `CoreRiskScoringPort` — `RuleBasedRiskEngine` (правила на базе DROOLS-подобных `RiskRule`).
- `CoreAiConfiguration` жёстко биндит `RuleBasedRiskEngine` как единственный бин.
- Нет ни одного клиента HTTP/Feign для общения с ML-сервисом.

Это вызывает вопросы у разработчиков: «Почему ML не подключён? Не потеряли ли мы что-то?»

---

## Решение

1. **`RuleBasedRiskEngine` остаётся дефолтной и единственной реализацией** `CoreRiskScoringPort` на неопределённый срок.
2. **ML-сервис считается опциональным** (status: `experimental`). Его доступность не влияет на работоспособность ядра системы.
3. **В будущем** ML будет вызываться через **отдельный порт** (например, `MlRiskScoringPort extends CoreRiskScoringPort`) и HTTP-клиент, который будет:
   - конвертировать `RiskFeatures` → `TelemetryPayload` (с учётом расхождения полей — см. ниже);
   - выполнять `POST /v1/predict`;
   - мапить `RiskScoreResponse` → `RiskScoringResult`.
4. Выбор между rule-based и ML-скорингом будет определяться конфигурацией (profile / feature flag), а не жёстким биндингом.

---

## Сравнение полей: ML-модель vs `RiskFeatures`

### Входные фичи ML-модели (`ml-service/train.py`, `FEATURE_COLUMNS`)

| # | Поле ML-модели | Тип | Есть в `RiskFeatures`? | Маппинг |
|---|----------------|-----|------------------------|---------|
| 1 | `temperature_c` | `float` | ✅ Да | `latestTemperatureCelsius` |
| 2 | `load_pct` | `float` | ✅ Да | `latestLoadPercent` |
| 3 | `vibration_mm_s` | `float` | ❌ **Нет** | Не собирается telemetry-репозиторием |
| 4 | `voltage_kv` | `float` | ❌ **Нет** | Не собирается telemetry-репозиторием |
| 5 | `humidity_pct` | `float` | ❌ **Нет** | Не собирается telemetry-репозиторием |
| 6 | `age_years` | `float` | ✅ Да | `assetAgeYears` |
| 7 | `failure_count_12m` | `int` | ❌ **Нет** | Есть `repairsLastYear` (другая семантика) |

### Поля `RiskFeatures`, которых нет в ML-модели

| Поле `RiskFeatures` | Назначение |
|---------------------|------------|
| `assetType` | Тип актива (enum) |
| `assetStatus` | Статус актива (enum) |
| `criticality` | Критичность актива (CRITICAL / HIGH / MEDIUM / LOW) |
| `latestOverheatingCount` | Количество перегреваний в последнем показании |
| `repairsLastYear` | Количество ремонтов за последний год |
| `averageTemperatureCelsius` | Среднее t за 24ч |
| `maxTemperatureCelsius` | Макс t за 24ч |
| `averageLoadPercent` | Средняя нагрузка за 24ч |
| `maxLoadPercent` | Макс нагрузка за 24ч |
| `overheatingEventsLast24Hours` | События перегрева за 24ч |
| `temperatureTrendCelsiusPerHour` | Тренд температуры за 24ч |
| `loadTrendPercentPerHour` | Тренд нагрузки за 24ч |

> **Наблюдание:** ML-модель использует 7 сырых полей из одного telemetry-снимка + `age_years`. Java-система собирает значительно больше агрегированных и трендовых признаков (16 полей в `RiskFeatures`), которые в текущей модели не используются.

---

## Открытый вопрос: `humidity_pct`

Поле `humidity_pct` присутствует в ML-модели, но **отсутствует** в `RiskFeatures` и не собирается `RiskFeaturesExtractor`:

- `TelemetryRecord` не хранит влажность.
- `TelemetryRepositoryPort` не возвращает влажность.
- Нет источника данных о влажности в текущей архитектуре.

**Рекомендация:** Перед интеграцией ML необходимо:
1. Определить источник данных о влажности (SCADA, IoT-датчики, внешний API).
2. Добавить поле `humidityCelsius` / `humidityPct` в `TelemetryRecord`.
3. Обновить `RiskFeaturesExtractor` для сбора влажности.
4. Рассмотреть переобучение модели с исключением `humidity_pct`, если источник недоступен.

---

## Аналогичные отсутствующие поля

| Поле | Причина отсутствия |
|------|---------------------|
| `vibration_mm_s` | Вибрация не регистрируется текущими telemetry-датчиками |
| `voltage_kv` | Напряжение не передаётся в telemetry-пайплайне |
| `failure_count_12m` | В системе есть `repairsLastYear` (ремонты), но не `failure_count` (отказы). Семантика различается |

---

## Последствия

### Положительные
- MVP-скоринг работает стабильно и предсказуемо (детерминированные правила).
- ML-сервис не является точкой отказа — он полностью изолирован.
- Разработчик видит чёткое объяснение: ML не подключен намеренно, а не из-за упущения.

### Отрицательные
- ML-модель не использует 11 из 16 полей `RiskFeatures` — потенциал данных недоиспользован.
- При будущей интеграции потребуется маппинг-слой между `RiskFeatures` и `TelemetryPayload`.
- Модели потребуется переобучение, если источники `vibration`, `voltage`, `humidity` не будут доступны.

---

## Альтернативы, которые были рассмотрены

| Альтернатива | Почему не выбрана |
|-------------|-------------------|
| Подключить ML как primary, rules как fallback | ML-модель на синтетических данных недостаточна для production; правила покрывают доменную логику |
| Переобучить модель на реальных данных с расширенными фичами | Нет достаточного объёма размеченных данных; `humidity`, `vibration`, `voltage` отсутствуют |
| Добавить ML-бейдж к `RuleBasedRiskEngine` напрямую | Нарушает SRP; `core.ai` не должен зависеть от внешнего HTTP-сервиса |

---

## Что нужно для будущей интеграции

1. [ ] Создать `MlRiskScoringPort` (или `MlClient`) в `infrastructure.ai.ml`.
2. [ ] Реализовать HTTP-клиент (WebClient / Feign) для `POST /v1/predict`.
3. [ ] Определить стратегию маппинга `RiskFeatures` → `TelemetryPayload` (какие поля использовать, что делать с missing).
4. [ ] Реализовать feature flag / config для переключения между `RuleBasedRiskEngine` и ML.
5. [ ] Добавить `humidity_pct` в telemetry-пайплайн **или** переобучить модель.
6. [ ] Написать интеграционные тесты для ML-клиента (mocked HTTP).

---

## Ссылки

- `ml-service/train.py` — определение `FEATURE_COLUMNS` (7 полей)
- `ml-service/app/schemas.py` — `TelemetryPayload` / `RiskScoreResponse`
- `ml-service/app/main.py` — эндпоинт `/v1/predict`
- `src/.../core/ai/RuleBasedRiskEngine.java` — текущая реализация
- `src/.../core/ai/CoreRiskScoringPort.java` — интерфейс порта
- `src/.../infrastructure/config/CoreAiConfiguration.java` — wiring
- `src/.../application/dto/RiskFeatures.java` — DTO входных признаков
- `src/.../application/service/RiskFeaturesExtractor.java` — извлечение признаков
