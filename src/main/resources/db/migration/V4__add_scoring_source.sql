-- Add scoring_source column to risk_assessments table
-- Identifies the scoring source: ML, FALLBACK, or RULES
ALTER TABLE risk_assessments ADD COLUMN scoring_source VARCHAR(32);
