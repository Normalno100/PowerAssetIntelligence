package com.powerassetintelligence.integration.service;

import com.powerassetintelligence.application.dto.TelemetryCreateCommand;
import com.powerassetintelligence.application.service.TelemetryService;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.infrastructure.persistence.entity.AssetEntity;
import com.powerassetintelligence.infrastructure.persistence.repository.AssetRepository;
import com.powerassetintelligence.infrastructure.persistence.repository.TelemetryRecordRepository;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Интеграционный тест на идемпотентность persist(TelemetryCreateCommand).
 *
 * <h3>Подход и обоснование</h3>
 *
 * Выбран подход «зафиксировать фактический дефект + описать ожидаемое поведение»
 * с аннотацией {@link Disabled}.
 *
 * <b>Почему @Disabled, а не @Test</b>:
 * <pre>{@code
 * Метод TelemetryService.persist() реализует паттерн "check-then-act":
 *   1) findByExternalTelemetryId(...)  — проверка существования
 *   2) persistNewRecord(...)            — вставка новой записи
 *
 * Оба шага находятся в одном @Transactional методе, НО @Transactional не
 * означает «один транзакционный контекст на все потоки». Каждый вызов
 * метода из нового потока получает СВОЮ транзакцию и СВОЁ JDBC-соединение.
 *
 * При параллельном вызове двух потоков с одинаковым externalTelemetryId:
 *   Поток A: SELECT ... WHERE external_telemetry_id = 'X'  → empty
 *   Поток B: SELECT ... WHERE external_telemetry_id = 'X'  → empty  (до commit потока A)
 *   Поток A: INSERT telemetry_records ...                    → success, commit
 *   Поток B: INSERT telemetry_records ...                    → DataIntegrityViolationException
 *                                                          (уникальный индекс idx_telemetry_external_id)
 *
 * Это классическая TOCTOU (time-of-check-time-of-use) гонка.
 * }
 * </pre>
 *
 * <b>Ожидаемое поведение (после исправления B2.2)</b>:
 * <ul>
 *   <li>Оба потока вызывают persist() с одинаковым externalTelemetryId</li>
 *   <li>В БД остаётся ровно одна запись</li>
 *   <li>Оба потока получают TelemetryResponse с одинаковым ID записи</li>
 *   <li>Никаких исключений</li>
 * </ul>
 *
 * <b>Необходимое исправление</b>:
 * <pre>{@code
 * Вариант 1 (рекомендуемый): перехват исключения на уровне адаптера
 *   TelemetryPersistenceAdapter.save() → catch DataIntegrityViolationException
 *   → findByExternalTelemetryId() → return existing
 *
 * Вариант 2: SELECT ... FOR UPDATE (pessimistic lock)
 *   Вариант 3: INSERT ... ON CONFLICT DO NOTHING (PostgreSQL upsert)
 * }</pre>
 *
 * <b>Критерии приёмки</b>:
 * <ul>
 *   <li>До B2.2: тест закомментирован / @Disabled, дефект зафиксирован</li>
 *   <li>После B2.2: снять @Disabled, тест зелёный</li>
 * </ul>
 *
 * <b>Профиль запуска</b>: {@code integration-tests} (failsafe + {@code -Pintegration-tests})
 *
 * <b>База данных</b>: H2 in-memory (PostgreSQL compatibility mode) через {@code application-test.properties}.
 * Для 100% надёжной репродукции гонки рекомендуется PostgreSQL + Testcontainers —
 * в H2 MVCC делает окно гонки очень узким (тест может пройти случайно).
 */
@Disabled("fails until B2.2: TOCTOU race in TelemetryService.persist() — "
        + "concurrent calls with same externalTelemetryId cause DataIntegrityViolationException "
        + "from unique index idx_telemetry_external_id")
class TelemetryIdempotencyIT extends BaseIntegrationTest {

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TelemetryRecordRepository telemetryRepository;

    /**
     * Два параллельных вызова TelemetryService.persist() с одинаковым externalTelemetryId
     * должны привести к одной записи в БД и вернуть один и тот же TelemetryResponse.
     *
     * <p>Фактическое поведение (до B2.2): второй поток падает с
     * DataIntegrityViolationException из-за unique index idx_telemetry_external_id.
     */
    @Test
    void concurrentPersistWithSameExternalId_shouldReturnSameRecordWithoutException()
            throws InterruptedException {

        // --- Arrange: создаём активный актив ---
        UUID assetId = UUID.randomUUID();
        AssetEntity asset = new AssetEntity(
                assetId,
                AssetType.TRANSFORMER,
                "TX-IDEMP-001",
                null,
                AssetStatus.ACTIVE,
                "Substation-Test",
                "TestManufacturer",
                AssetCriticality.HIGH,
                30,
                Collections.emptyMap()
        );
        assetRepository.saveAndFlush(asset);

        // --- Подготовка команды ---
        String externalId = "EXTP-CONCURRENT-" + UUID.randomUUID();
        Instant now = Instant.now();

        TelemetryCreateCommand command = new TelemetryCreateCommand(
                assetId,
                now,
                BigDecimal.valueOf(75.50),
                BigDecimal.valueOf(60.00),
                BigDecimal.valueOf(110.000),
                BigDecimal.valueOf(150.250),
                BigDecimal.valueOf(2.500),
                0,
                "sensor-001",
                externalId
        );

        // --- Синхронизация потоков ---
        // latch — оба потока ждут здесь перед вызовом persist()
        CountDownLatch startLatch = new CountDownLatch(1);
        // barrier — сигнализирует, что оба потока завершены
        CountDownLatch completionLatch = new CountDownLatch(2);

        // Хранилища результатов
        AtomicReference<Throwable> errorA = new AtomicReference<>();
        AtomicReference<Throwable> errorB = new AtomicReference<>();
        AtomicReference<Object> responseA = new AtomicReference<>();
        AtomicReference<Object> responseB = new AtomicReference<>();

        // --- Act: параллельный вызов ---
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            // Поток A
            executor.submit(() -> {
                try {
                    startLatch.await(); // ждём сигнала "старт"
                    responseA.set(telemetryService.persist(command));
                } catch (Throwable t) {
                    errorA.set(t);
                } finally {
                    completionLatch.countDown();
                }
            });

            // Поток B
            executor.submit(() -> {
                try {
                    startLatch.await(); // ждём сигнала "старт"
                    responseB.set(telemetryService.persist(command));
                } catch (Throwable t) {
                    errorB.set(t);
                } finally {
                    completionLatch.countDown();
                }
            });

            // Сигнал "старт" обоим потокам одновременно
            startLatch.countDown();
            // Ждём завершения обоих потоков
            completionLatch.await();
        }

        // --- Assert: ни один из потоков не должен получить исключение ---
        List<Throwable> errors = List.of(errorA.get(), errorB.get())
                .stream()
                .filter(t -> t != null)
                .toList();

        if (!errors.isEmpty()) {
            // Фиксируем фактическое поведение для отладки
            throw new AssertionError(
                    "ОЖИДАЕТСЯ: оба потока возвращают TelemetryResponse без исключений. "
                            + "ФАКТ: получено " + errors.size() + " исключение(ий). "
                            + "Причина: TOCTOU race в TelemetryService.persist(). "
                            + "Исправление: B2.2 — перехват DataIntegrityViolationException "
                            + "в TelemetryPersistenceAdapter или SELECT FOR UPDATE."
                            + System.lineSeparator()
                            + errors.stream()
                                    .map(t -> t.getClass().getSimpleName() + ": " + t.getMessage())
                                    .reduce((a, b) -> a + "; " + b)
                                    .orElse("")
            );
        }

        // Оба ответа не null
        org.junit.jupiter.api.Assertions.assertNotNull(
                responseA.get(), "Ответ потока A не должен быть null");
        org.junit.jupiter.api.Assertions.assertNotNull(
                responseB.get(), "Ответ потока B не должен быть null");

        // Оба ответа ссылаются на одну и ту же запись (по ID)
        // Приводим к общему типу — оба это TelemetryResponse
        UUID idA = extractId(responseA.get());
        UUID idB = extractId(responseB.get());

        org.junit.jupiter.api.Assertions.assertEquals(
                idA, idB,
                "Оба потока должны получить ответ с одинаковым ID записи"
        );

        // В БД ровно одна запись с данным externalTelemetryId
        org.junit.jupiter.api.Assertions.assertEquals(
                1L,
                telemetryRepository.count(),
                "В БД должна быть ровно одна запись с данным externalTelemetryId"
        );

        // Верифицируем через прямой query
        org.junit.jupiter.api.Assertions.assertTrue(
                telemetryRepository.findByExternalTelemetryId(externalId).isPresent(),
                "Запись с externalTelemetryId должна существовать"
        );
    }

    /**
     * Вспомогательный метод для извлечения ID из ответа.
     * Работает с TelemetryResponse (domain record).
     */
    private static UUID extractId(Object response) {
        if (response == null) {
            return null;
        }
        // TelemetryResponse — это record с первым полем UUID id
        try {
            java.lang.reflect.Method idMethod = response.getClass().getMethod("id");
            return (UUID) idMethod.invoke(response);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось извлечь ID из ответа: " + response.getClass(), e);
        }
    }
}
