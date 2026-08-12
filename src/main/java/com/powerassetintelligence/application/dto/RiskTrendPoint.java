package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.core.ai.TrendDirection;
import com.powerassetintelligence.domain.model.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Точка истории изменения риска для актива.
 * Представляет отдельный снимок оценки риска в конкретный момент времени.
 */
public record RiskTrendPoint(
    /** ID оценки риска */
    UUID assessmentId,
    /** ID актива, к которому относится оценка */
    UUID assetId,
    /** Дата и время проведения оценки */
    Instant assessedAt,
    /** Численный показатель риска (score) */
    BigDecimal riskScore,
    /** Уровень риска на момент оценки */
    RiskLevel riskLevel,
    /** Изменение показателя риска по сравнению с предыдущей оценкой (null если это первая оценка) */
    BigDecimal scoreChange,
    /** Направление изменения риска: RISING, FALLING, STABLE */
    TrendDirection trend
) {}
