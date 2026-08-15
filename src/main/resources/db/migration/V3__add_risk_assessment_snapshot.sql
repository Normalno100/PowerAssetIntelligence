-- Add risk_assessment_snapshot column for audit and reproducibility.
-- This stores the exact input features used to compute each assessment as JSON.
-- Nullable for backward compatibility: assessments created before this migration
-- will have NULL snapshots and remain queryable via existing APIs.

ALTER TABLE risk_assessments
    ADD COLUMN risk_assessment_snapshot TEXT;

-- Optionally add a comment documenting the column
COMMENT ON COLUMN risk_assessments.risk_assessment_snapshot IS
    'JSON snapshot of RiskFeatures used to compute the assessment; enables full reproducibility and audit';
