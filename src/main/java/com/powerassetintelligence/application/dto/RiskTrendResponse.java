package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.core.ai.TrendDirection;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Агрегированный ответ по тренду риска для актива.
 * Содержит историю оценок и сводную статистку изменений.
 */
public record RiskTrendResponse(
    /** ID актива, к которому относится тренд */
    UUID assetId,
    /** Полный список точек истории изменений риска */
    List<RiskTrendPoint> points,
    /** Текущий (последний по времени) показатель риска */
    BigDecimal currentScore,
    /** Предыдущий (предпоследний по времени) показатель риска */
    BigDecimal previousScore,
    /** Общее изменение показателя риска за весь период (currentScore - previousScore) */
    BigDecimal totalChange,
    /** Среднее изменение показателя риска между последовательными оценками */
    BigDecimal averageChange,
    /** Общее направление тренда: RISING, FALLING, STABLE */
    TrendDirection direction
) {}
