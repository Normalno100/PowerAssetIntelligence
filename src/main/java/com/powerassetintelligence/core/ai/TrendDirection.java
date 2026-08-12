package com.powerassetintelligence.core.ai;

/**
 * Direction of risk trend between two consecutive assessments.
 * Uses a tolerance band to avoid flagging minor fluctuations.
 */
public enum TrendDirection {
    /** Risk score increased compared to previous assessment */
    RISING,
    /** Risk score decreased compared to previous assessment */
    FALLING,
    /** Risk score is stable within tolerance band */
    STABLE
}
