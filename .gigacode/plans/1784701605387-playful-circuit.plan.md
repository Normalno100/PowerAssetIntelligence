# План рефакторинга Clean Architecture

## Проблемы

### 1. Spring Data Page/Pageable в application layer
**Файлы:**
- `AssetRepositoryPort.java` — импорт `org.springframework.data.domain.Page`, `Pageable`
- `MaintenanceRepositoryPort.java` — импорт `Page`, `Pageable`
- `RiskAssessmentRepositoryPort.java` — импорт `Page`, `Pageable`
- `TelemetryRepositoryPort.java` — импорт `Page`, `Pageable`

**Проблема:** Application layer зависит от Spring Data Commons — нарушение Clean Architecture.

**Решение:** Создать собственные абстракции `PageRequest`, `PageResult`, `SortOrder`.

### 2. save(..., Asset asset) — лишний параметр
**Файлы:**
- `MaintenanceRepositoryPort.java`: `save(MaintenanceRecord, Asset)`
- `RiskAssessmentRepositoryPort.java`: `save(RiskAssessment, Asset)`
- `TelemetryRepositoryPort.java`: `save(TelemetryRecord, Asset)`

**Проблема:** Repository port должен работать с агрегатом целиком, но текущие методы принимают сущность + внешний Asset.

**Решение:** 
- `MaintenanceRecord`, `RiskAssessment`, `TelemetryRecord` содержат `assetId` — достаточно передавать только сущность
- Удалить параметр `Asset asset` из всех методов save

### 3. RiskScoringPort зависит от core.ai
**Файлы:**
- `RiskScoringPort.java` импортирует `RiskFeatures`, `RiskScoringResult` из `core.ai`

**Проблема:** Инверсия зависимостей — application импортирует из core.ai, а должно быть наоборот.

**Решение:** Перенести DTO в `application.port.out` или `application.dto`, пусть core.ai зависит от application.

---

## Детальный план

### Шаг 1: Создать pagination abstractions в application layer

**Создать файлы:**
1. `src/main/java/com/powerassetintelligence/application/port/out/Pagination.java` — `PageRequest`, `PageResult`, `SortOrder`

```java
package com.powerassetintelligence.application.port.out;

import java.util.List;
import java.util.Objects;

public record PageRequest(int page, int size, List<SortOrder> sort) {
    public record SortOrder(String field, Direction direction) {
        public enum Direction { ASC, DESC }
    }
}

public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
```

2. `src/main/java/com/powerassetintelligence/application/dto/AssetSearchCriteria.java` — критерии поиска (если нужно)

---

### Шаг 2: Обновить все repository ports

**Изменить файлы:**
1. `AssetRepositoryPort.java`
   - Убрать импорт Spring Data
   - Заменить `Page<Asset> search(..., Pageable pageable)` на `PageResult<Asset> search(AssetType type, AssetStatus status, AssetCriticality criticality, String location, PageRequest pageRequest)`

2. `MaintenanceRepositoryPort.java`
   - Убрать импорт Spring Data
   - Убрать параметр `Asset asset` из `save()`
   - Заменить `Page<MaintenanceRecord> findByAssetId(..., Pageable pageable)` на `PageResult<MaintenanceRecord> findByAssetId(UUID assetId, PageRequest pageRequest)`

3. `RiskAssessmentRepositoryPort.java`
   - Убрать импорт Spring Data
   - Убрать параметр `Asset asset` из `save()`
   - Заменить `Page<RiskAssessment> findByAssetId(..., Pageable pageable)` на `PageResult<RiskAssessment> findByAssetId(UUID assetId, PageRequest pageRequest)`
   - Заменить `Page<RiskAssessment> findAll(..., Pageable pageable)` на `PageResult<RiskAssessment> findAll(PageRequest pageRequest)`

4. `TelemetryRepositoryPort.java`
   - Убрать импорт Spring Data
   - Убрать параметр `Asset asset` из `save()`
   - Заменить `Page<TelemetryRecord> findByAssetId(..., Pageable pageable)` на `PageResult<TelemetryRecord> findByAssetId(UUID assetId, PageRequest pageRequest)`

---

### Шаг 3: Перенести RiskFeatures и RiskScoringResult в application layer

**Вариант А (рекомендуется):** Создать новые DTO в `application.dto`
**Вариант Б:** Оставить в `application.port.out` как part of the contract

**Создать:**
1. `src/main/java/com/powerassetintelligence/application/dto/RiskFeatures.java` (копия из core.ai)
2. `src/main/java/com/powerassetintelligence/application/dto/RiskScoringResult.java` (копия из core.ai)

**Изменить:**
- `RiskScoringPort.java`: импорты из `application.dto` вместо `core.ai`

**Изменить в core.ai:**
- `RiskFeatures.java`: импорты из `application.dto` (или оставить как есть, если перенесли DTO)
- `RiskScoringResult.java`: импорты из `application.dto`
- `RuleBasedRiskEngine.java`: импорты из `application.dto`

---

### Шаг 4: Обновить persistence adapters

**Изменить файлы:**
1. `AssetPersistenceAdapter.java`
   - В методе `search`: маппинг `PageRequest` → `org.springframework.data.domain.Pageable` и `Page` → `PageResult`
   
2. `MaintenancePersistenceAdapter.java`
   - Убрать зависимость от `AssetRepository` (если не используется для других операций)
   - Убрать параметр `Asset asset` из `save()`
   - В `findByAssetId`: маппинг `PageRequest` → `Pageable` и `Page` → `PageResult`

3. `RiskAssessmentPersistenceAdapter.java`
   - Убрать зависимость от `AssetRepository` (если не используется для других операций)
   - Убрать параметр `Asset asset` из `save()`
   - В методах с `Pageable`: маппинг

4. `TelemetryPersistenceAdapter.java`
   - Убрать зависимость от `AssetRepository` (если не используется для других операций)
   - Убрать параметр `Asset asset` из `save()`
   - В методе с `Pageable`: маппинг

---

### Шаг 5: Обновить application services (если есть)

Найти и обновить все use-case services, которые вызывают repository methods:
- `src/main/java/com/powerassetintelligence/application/service/`

---

### Шаг 6: Обновить Infrastructure layer (если нужно)

Проверить `AssetRepository.java`, `MaintenanceRecordRepository.java`, `RiskAssessmentRepository.java`, `TelemetryRecordRepository.java`:
- Убедиться, что методы возвращают Spring Data `Page` (адаптеры сделают маппинг)

---

## Файлы для создания

1. `Pagination.java` — shared types
2. `AssetSearchCriteria.java` (опционально)
3. `RiskFeatures.java` в `application.dto`
4. `RiskScoringResult.java` в `application.dto`

## Файлы для изменения

**Application layer:**
- `AssetRepositoryPort.java`
- `MaintenanceRepositoryPort.java`
- `RiskAssessmentRepositoryPort.java`
- `TelemetryRepositoryPort.java`
- `RiskScoringPort.java`

**Infrastructure layer:**
- `AssetPersistenceAdapter.java`
- `MaintenancePersistenceAdapter.java`
- `RiskAssessmentPersistenceAdapter.java`
- `TelemetryPersistenceAdapter.java`

**Core layer:**
- `RiskFeatures.java`
- `RiskScoringResult.java`
- `RuleBasedRiskEngine.java`

---

## Проверка

1. Компиляция: `mvn clean compile`
2. Unit-тесты: `mvn test` (убедиться, что tests не требуют Spring Data на classpath)
3. Проверить, что в application layer нет импортов `org.springframework.data`
4. Проверить направление зависимостей: `core.ai` → `application` (а не наоборот)

---

## Примечания

- `PageRequest` и `PageResult` — value objects, могут быть immutable records
- Маппинг в persistence adapter должен быть корректным (учесть `totalElements`, `totalPages`)
- Если `SortOrder` не нужен — можно упростить `PageRequest` до `(int page, int size)`
