create table risk_assessments (
    id uuid not null,
    asset_id uuid not null,
    assessed_at timestamp with time zone not null,
    risk_score numeric(5, 2) not null,
    risk_level varchar(32) not null,
    model_version varchar(64) not null,
    explanation varchar(4000) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint fk_risk_assessments_asset foreign key (asset_id) references assets (id) on delete restrict,
    constraint chk_risk_assessments_score check (risk_score >= 0 and risk_score <= 100),
    constraint chk_risk_assessments_level check (risk_level in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

create table risk_assessment_factors (
    risk_assessment_id uuid not null,
    factor varchar(1000) not null,
    constraint fk_risk_assessment_factors_assessment foreign key (risk_assessment_id) references risk_assessments (id) on delete cascade
);

create table risk_assessment_recommendations (
    risk_assessment_id uuid not null,
    recommendation varchar(1000) not null,
    constraint fk_risk_assessment_recommendations_assessment foreign key (risk_assessment_id) references risk_assessments (id) on delete cascade
);

create index idx_risk_assessments_asset_assessed_at on risk_assessments (asset_id, assessed_at desc);
create index idx_risk_assessments_level_score on risk_assessments (risk_level, risk_score desc);
