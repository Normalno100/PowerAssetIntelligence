create table assets (
    id uuid not null,
    type varchar(64) not null,
    name varchar(255) not null,
    installation_date date not null,
    status varchar(64) not null,
    location varchar(512) not null,
    manufacturer varchar(255) not null,
    criticality varchar(64) not null,
    expected_service_life_years integer not null,
    version bigint not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    primary key (id),
    constraint chk_assets_type check (type in (
        'TRANSFORMER', 'SUBSTATION', 'CIRCUIT_BREAKER', 'OVERHEAD_LINE', 'CABLE_LINE', 'SWITCHGEAR', 'SENSOR'
    )),
    constraint chk_assets_status check (status in (
        'ACTIVE', 'WARNING', 'CRITICAL', 'UNDER_MAINTENANCE', 'DECOMMISSIONED'
    )),
    constraint chk_assets_criticality check (criticality in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    constraint chk_assets_service_life check (expected_service_life_years > 0)
);

create table asset_technical_parameters (
    asset_id uuid not null,
    parameter_name varchar(128) not null,
    parameter_value varchar(512),
    primary key (asset_id, parameter_name),
    constraint fk_asset_technical_parameters_asset foreign key (asset_id) references assets (id) on delete cascade
);

create table telemetry_records (
    id uuid not null,
    asset_id uuid not null,
    recorded_at timestamp with time zone not null,
    temperature_celsius numeric(8, 2),
    load_percent numeric(8, 2),
    voltage_kv numeric(10, 3),
    current_ampere numeric(12, 3),
    vibration_mm_sec numeric(8, 3),
    overheating_count integer,
    source_sensor_id varchar(128) not null,
    external_telemetry_id varchar(128),
    created_at timestamp with time zone not null,
    primary key (id),
    constraint fk_telemetry_asset foreign key (asset_id) references assets (id) on delete restrict,
    constraint chk_telemetry_temperature check (temperature_celsius is null or temperature_celsius between -80 and 250),
    constraint chk_telemetry_load check (load_percent is null or load_percent between 0 and 150),
    constraint chk_telemetry_voltage check (voltage_kv is null or voltage_kv >= 0),
    constraint chk_telemetry_current check (current_ampere is null or current_ampere >= 0),
    constraint chk_telemetry_vibration check (vibration_mm_sec is null or vibration_mm_sec >= 0),
    constraint chk_telemetry_overheating check (overheating_count is null or overheating_count >= 0)
);

create table maintenance_records (
    id uuid not null,
    asset_id uuid not null,
    repair_date date not null,
    maintenance_type varchar(64) not null,
    description varchar(2000) not null,
    repair_cost numeric(14, 2) not null,
    failure_code varchar(64),
    performed_by varchar(255) not null,
    created_at timestamp with time zone not null,
    primary key (id),
    constraint fk_maintenance_asset foreign key (asset_id) references assets (id) on delete restrict,
    constraint chk_maintenance_type check (maintenance_type in (
        'INSPECTION', 'DIAGNOSTICS', 'REPAIR', 'COMPONENT_REPLACEMENT', 'EMERGENCY_REPAIR'
    )),
    constraint chk_maintenance_repair_cost check (repair_cost >= 0)
);

create table maintenance_replaced_components (
    maintenance_record_id uuid not null,
    component varchar(255) not null,
    constraint fk_replaced_components_maintenance foreign key (maintenance_record_id) references maintenance_records (id) on delete cascade
);

create index idx_assets_type_status on assets (type, status);
create index idx_assets_criticality on assets (criticality);
create index idx_assets_location on assets (location);
create index idx_telemetry_asset_timestamp on telemetry_records (asset_id, recorded_at desc);
create unique index idx_telemetry_external_id on telemetry_records (external_telemetry_id);
create index idx_maintenance_asset_repair_date on maintenance_records (asset_id, repair_date desc);
create index idx_maintenance_type on maintenance_records (maintenance_type);
