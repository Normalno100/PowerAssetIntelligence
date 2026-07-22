# 🏭 AI-платформа интеллектуального управления активами электрических сетей

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=java)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=spring)](https://spring.io/)
[![License](https://img.shields.io/badge/License-Proprietary-E91E63)]()

**Power Asset Intelligence** — это интеллектуальная цифровая платформа для мониторинга состояния оборудования электрических сетей, прогнозирования рисков отказов и оптимизации обслуживания активов с использованием технологий искусственного интеллекта.

## ✨ Основные возможности

- 📊 **Мониторинг оборудования** — телеметрия по температуре, нагрузке, напряжению, вибрации
- 🤖 **AI-анализ** — rule-based система оценки рисков и предиктивная модель (ML)
- 📈 **Прогноз отказов** — вероятность выхода из строя в ближайшие 30 дней
- 🎯 **Приоритизация** — ранжирование активов по уровню критичности
- 🔧 **История обслуживания** — полный аудит ремонтов и замененных компонентов
- 📋 **Отчёты и аналитика** — dashboard с метриками и инсайтами

---

## 🏗 Архитектура

Проект следует принципам **Clean Architecture** с разделением на слои:

```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure                           │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │ REST API │  │  Persistence │  │    Messaging (Kafka)  │ │
│  └──────────┘  └──────────────┘  └───────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                         ↑
┌─────────────────────────────────────────────────────────────┐
│                     Application                             │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐ │
│  │ Use Cases    │  │ DTOs         │  │ Application Ports │ │
│  └──────────────┘  └──────────────┘  └───────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                         ↑
┌─────────────────────────────────────────────────────────────┐
│                        Domain                               │
│  ┌─────────┐  ┌───────────┐  ┌─────────────┐  ┌──────────┐ │
│  │  Asset  │  │ Telemetry │  │ Maintenance │  │  Risk    │ │
│  └─────────┘  └───────────┘  └─────────────┘  └──────────┘ │
└─────────────────────────────────────────────────────────────┘
                         ↑
┌─────────────────────────────────────────────────────────────┐
│                         Core                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │        AI/ML Risk Engine (Rule-based + ML)           │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Технологический стек

| Слой | Технологии |
|------|-----------|
| **Backend** | Java 21, Spring Boot 3.3.5, Spring Data JPA, Spring Kafka |
| **База данных** | PostgreSQL 16, Flyway migrations |
| **AI/ML** | Python 3.12, FastAPI, scikit-learn, Pandas |
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS, Chart.js |
| **Infrastructure** | Docker, Docker Compose, Kafka, Prometheus, Loki, Grafana |
| **Тестирование** | JUnit 5, Mockito, Testcontainers, MockMvc |

---

## 🚀 Быстрый старт

### Требования

- Docker и Docker Compose
- Java 21+ (для локальной разработки backend)
- Node.js 18+ (для frontend)
- Python 3.12+ (для ML-сервиса)

### Запуск через Docker (рекомендуется)

```bash
# Клонирование репозитория
git clone <repository-url>
cd PowerAssetIntelligence

# Создание .env файла
cp .env.example .env

# Запуск всех сервисов
docker compose up -d --build
```

### Доступные сервисы

| Сервис | Порт | Описание |
|--------|------|----------|
| API | `http://localhost:8080` | REST API |
| Frontend | `http://localhost:3001` | Web UI |
| API Health | `/actuator/health` | Статус приложения |
| Swagger UI | `/swagger-ui.html` | Документация API |
| Prometheus | `http://localhost:9090` | Метрики |
| Grafana | `http://localhost:3000` | Визуализация (admin/admin) |
| ML Service | `http://localhost:8000` | ML инференс |

### Остановка

```bash
docker compose down
```

### Просмотр логов

```bash
# Все сервисы
docker compose logs -f

# Конкретный сервис
docker compose logs -f app
docker compose logs -f ml-service
```

---

## 📖 Использование API

### Управление активами

#### Создание актива

```http
POST /api/v1/assets
Content-Type: application/json

{
  "type": "TRANSFORMER",
  "name": "Трансформатор ТМ-1000/110",
  "installationDate": "2015-03-15",
  "location": "Подстанция 110кВ Северная",
  "manufacturer": "Завод Электросила",
  "criticality": "HIGH",
  "expectedServiceLifeYears": 30,
  "technicalParameters": {
    "power_kva": "1000",
    "voltage_kv": "110"
  }
}
```

#### Список активов

```http
GET /api/v1/assets?type=TRANSFORMER&status=ACTIVE&criticality=HIGH&location=Северная&page=0&size=20
```

#### Оценка риска

```http
GET /api/v1/risk-analysis/{assetId}
```

Ответ содержит:
- `riskScore` — числовая оценка риска (0-100)
- `riskLevel` — CRITICAL / HIGH / MEDIUM / LOW
- `riskFactors` — список факторов, повлиявших на оценку
- `recommendations` — рекомендации по действию

### Загрузка телеметрии

```http
POST /api/v1/telemetry
Content-Type: application/json

{
  "assetId": "uuid-here",
  "timestamp": "2026-07-22T10:30:00Z",
  "temperatureCelsius": 85.5,
  "loadPercent": 92.0,
  "voltageKv": 112.3,
  "currentAmpere": 520.5,
  "vibrationMmSec": 4.2,
  "overheatingCount": 1,
  "sourceSensorId": "TEMP_SENSOR_001",
  "externalTelemetryId": "TELEM-20260722-001"
}
```

---

## 🧠 AI/ML модуль

### Rule-based система

На первом этапе используется система правил для оценки рисков:

```
Если возраст > 15 лет И температура > 80°C И ремонтов > 3/год → РИСК = CRITICAL
Если нагрузка > 90% И частые перегревы → Проверка системы охлаждения
Если температура > 95°C → Немедленная проверка
```

### ML-модель

В перспективе — обученная модель на основе scikit-learn для прогнозирования вероятности отказа.

#### Обучение модели

```bash
cd ml-service
python train.py
```

#### ML API эндпоинты

```http
GET /health              # Статус сервиса
POST /v1/predict         # Предсказание вероятности отказа
```

---

## 🧪 Тестирование

### Unit-тесты

```bash
mvn test
```

### Integration-тесты

```bash
mvn verify -Pintegration-tests
```

### Покрытие кода

```bash
mvn clean test jacoco:report
# Отчет: target/site/jacoco/index.html
```

### Тестовая стратегия

- **Unit tests** — бизнес-логика, маппинг, валидация
- **Integration tests** — API контракты, провязка сервисов
- **Kafka tests** — producer/consumer paths
- **Database tests** — constraints, миграции, запросы

---

## 📚 Документация

- [Техническое задание](#Техническое%20задание%20(ТЗ).txt)
- [План развития](#План%20развития%20проекта)
- [Clean Architecture compliance](docs/clean-architecture-compliance.md)
- [Test Strategy](docs/testing/TEST_STRATEGY.md)

---

## 🛠 Локальная разработка

### Backend (Java)

```bash
# Сборка без тестов
mvn clean package -DskipTests

# Запуск в IDE
# PowerAssetIntelligenceApplication.java

# Запуск через Maven
mvn spring-boot:run
```

### Frontend (React)

```bash
cd frontend

# Установка зависимостей
npm install

# Запуск dev-сервера
npm run dev

# Сборка
npm run build

# Превью сборки
npm run preview
```

### ML Service (Python)

```bash
cd ml-service

# Установка зависимостей
pip install -r requirements.txt

# Обучение модели
python train.py

# Запуск API
uvicorn app.main:app --reload --port 8000
```

---

## 📊 Основные сущности

### Asset (Актив)

```java
- id: UUID
- type: AssetType (TRANSFORMER, SUBSTATION, CIRCUIT_BREAKER, etc.)
- name: String
- installationDate: LocalDate
- status: AssetStatus (ACTIVE, WARNING, CRITICAL, etc.)
- location: String
- manufacturer: String
- criticality: AssetCriticality (LOW, MEDIUM, HIGH, CRITICAL)
- expectedServiceLifeYears: Integer
- technicalParameters: Map<String, String>
```

### TelemetryRecord

```java
- id: UUID
- assetId: UUID
- timestamp: Instant
- temperatureCelsius: BigDecimal
- loadPercent: BigDecimal
- voltageKv: BigDecimal
- currentAmpere: BigDecimal
- vibrationMmSec: BigDecimal
- overheatingCount: Integer
```

### RiskAssessment

```java
- id: UUID
- assetId: UUID
- riskScore: BigDecimal (0-100)
- riskLevel: RiskLevel
- riskFactors: List<String>
- recommendations: List<String>
- modelVersion: String
```

---

## 🔄 CI/CD

Текущий статус: 🚧 В разработке

Планируемые этапы:
1. Локальная разработка и тестирование
2. GitHub Actions для CI (build, test, lint)
3. Docker push в registry
4. Деплой в K8s через Helm

---

## 📝 План развития

### Этап 1 (MVP) — ✅ Завершено
- [x] Управление активами
- [x] Телеметрия и Kafka
- [x] Rule-based AI
- [x] REST API и Swagger
- [x] Docker infrastructure

### Этап 2 — В разработке
- [ ] Frontend dashboard
- [ ] Аналитика и отчёты
- [ ] Интеграция с внешними системами
- [ ] Экспорт данных (CSV, Excel)

### Этап 3 — Планируется
- [ ] Обученные ML-модели
- [ ] Anomaly detection
- [ ] Predictive analytics
- [ ] Explainable AI (SHAP/LIME)
- [ ] Microservices с event-driven архитектурой

---

## 🤝 Вклад в проект

Мы приветствуем вклад! Пожалуйста:

1. Форкните репозиторий
2. Создайте ветку (`git checkout -b feature/AmazingFeature`)
3. Закоммитьте изменения (`git commit -m 'Add some AmazingFeature'`)
4. Отправьте в ветку (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request

---

## 📄 Лицензия

Этот проект является проприетарным. Все права защищены © Power Asset Intelligence 2026.

---

## 📞 Поддержка

Для вопросов и предложений создавайте issues в репозитории проекта или свяжитесь с командой разработки.

---

**Спасибо за использование Power Asset Intelligence!** 🚀
