# Проверка соответствия Clean Architecture

Дата проверки: 2026-06-23.

## Итог после исправлений

Проект приведен ближе к целевой архитектуре из `docs/clean-architecture.md`: application layer больше не импортирует JPA entities/repositories из infrastructure, AI-core больше не зависит от Spring, а domain layer получил чистые модели `Asset`, `TelemetryRecord`, `MaintenanceRecord` и `RiskAssessment`. Инфраструктура теперь подключается к application через output ports и persistence adapters.

## Исправлено

- Введены output ports в `application.port.out`: `AssetRepositoryPort`, `TelemetryRepositoryPort`, `MaintenanceRepositoryPort`, `RiskAssessmentRepositoryPort`, `RiskScoringPort`.
- Application services зависят от ports и чистых domain models, а не от `infrastructure.persistence.entity` и Spring Data repositories.
- Добавлены domain models без Spring/JPA-аннотаций для активов, телеметрии, ремонтов и risk assessments.
- Добавлены persistence adapters в `infrastructure.persistence.adapter`, которые реализуют output ports и мапят JPA entities в domain models через `PersistenceMapper`.
- `RuleBasedRiskEngine` отвязан от Spring; Spring-компонентом является infrastructure adapter `RuleBasedRiskScoringAdapter`, реализующий `RiskScoringPort`.
- Из application request DTO удалены `jakarta.validation` annotations, чтобы application layer не зависел от web validation API.

## Оставшиеся улучшения

- REST request DTO стоит окончательно вынести в `infrastructure.web.dto`, чтобы восстановить bean validation на web-boundary без протекания Jakarta Validation в application layer.
- Input ports (`RegisterAssetUseCase`, `RecordMaintenanceUseCase`, `IngestTelemetryUseCase`, `AssessRiskUseCase`) еще не выделены отдельными интерфейсами; контроллеры продолжают зависеть от concrete application services.
- Следующий шаг — добавить ArchUnit-тесты, фиксирующие правила: domain не зависит от Spring/JPA; application не зависит от infrastructure; core.ai не зависит от Spring; infrastructure может зависеть от application/domain.
