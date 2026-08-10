package com.powerassetintelligence.core.ai;

/**
 * Direction of risk change between two consecutive assessments.
 */
public enum RiskChangeDirection {
    /** Risk score increased compared to previous assessment */
    INCREASED,
    /** Risk score decreased compared to previous assessment */
    DECREASED,
    /** Risk score is the same as previous assessment */
    UNCHANGED,
    /** No previous assessment exists — comparison is not possible */
    NO_PREVIOUS_ASSESSMENT
}
