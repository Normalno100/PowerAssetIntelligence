package com.powerassetintelligence.unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerassetintelligence.domain.model.Asset;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssetDomainTest {

    private Asset createAsset(AssetStatus initialStatus) {
        UUID id = UUID.randomUUID();
        return new Asset(
                id,
                AssetType.TRANSFORMER,
                "TX-001",
                LocalDate.of(2020, 1, 1),
                initialStatus,
                "Substation-North",
                "ABB",
                AssetCriticality.HIGH,
                30,
                Map.of("kv", "110")
        );
    }

    // ---- decommission() ----

    @Nested
    @DisplayName("decommission()")
    class DecommissionTests {

        @Test
        @DisplayName("should transition ACTIVE -> DECOMMISSIONED")
        void decommission_fromActive_setsDecommissioned() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            asset.decommission();
            assertEquals(AssetStatus.DECOMMISSIONED, asset.getStatus());
        }

        @Test
        @DisplayName("should be no-op when already DECOMMISSIONED")
        void decommission_alreadyDecommissioned_isNoOp() {
            Asset asset = createAsset(AssetStatus.DECOMMISSIONED);
            asset.decommission();
            assertEquals(AssetStatus.DECOMMISSIONED, asset.getStatus());
        }

        @Test
        @DisplayName("should transition UNDER_MAINTENANCE -> DECOMMISSIONED")
        void decommission_fromUnderMaintenance_setsDecommissioned() {
            Asset asset = createAsset(AssetStatus.UNDER_MAINTENANCE);
            asset.decommission();
            assertEquals(AssetStatus.DECOMMISSIONED, asset.getStatus());
        }
    }

    // ---- startMaintenance() ----

    @Nested
    @DisplayName("startMaintenance()")
    class StartMaintenanceTests {

        @Test
        @DisplayName("should transition ACTIVE -> UNDER_MAINTENANCE")
        void startMaintenance_fromActive() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            asset.startMaintenance();
            assertEquals(AssetStatus.UNDER_MAINTENANCE, asset.getStatus());
        }

        @Test
        @DisplayName("should be no-op when already DECOMMISSIONED")
        void startMaintenance_alreadyDecommissioned_isNoOp() {
            Asset asset = createAsset(AssetStatus.DECOMMISSIONED);
            asset.startMaintenance();
            assertEquals(AssetStatus.DECOMMISSIONED, asset.getStatus());
        }
    }

    // ---- updateFields() ----

    @Nested
    @DisplayName("updateFields()")
    class UpdateFieldsTests {

        @Test
        @DisplayName("should throw when name is blank")
        void updateFields_withBlankName_throws() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            assertThrows(IllegalArgumentException.class,
                    () -> asset.updateFields("  ", null, null, null, null, null, null, null));
            assertEquals("TX-001", asset.getName());
        }

        @Test
        @DisplayName("should throw when expectedServiceLifeYears <= 0")
        void updateFields_withNegativeServiceLife_throws() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            assertThrows(IllegalArgumentException.class,
                    () -> asset.updateFields(null, null, null, null, null, null, -1, null));
        }

        @Test
        @DisplayName("should throw when expectedServiceLifeYears == 0")
        void updateFields_withZeroServiceLife_throws() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            assertThrows(IllegalArgumentException.class,
                    () -> asset.updateFields(null, null, null, null, null, null, 0, null));
        }

        @Test
        @DisplayName("should update valid fields successfully")
        void updateFields_validFields_succeeds() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            asset.updateFields(
                    "TX-Updated",
                    null,
                    null,
                    "Substation-South",
                    "Siemens",
                    AssetCriticality.MEDIUM,
                    25,
                    Map.of("kv", "220")
            );
            assertEquals("TX-Updated", asset.getName());
            assertEquals("Substation-South", asset.getLocation());
            assertEquals("Siemens", asset.getManufacturer());
            assertEquals(AssetCriticality.MEDIUM, asset.getCriticality());
            assertEquals(Integer.valueOf(25), asset.getExpectedServiceLifeYears());
            assertEquals("220", asset.getTechnicalParameters().get("kv"));
        }

        @Test
        @DisplayName("should trim name and location when updating")
        void updateFields_trimsFields() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            asset.updateFields(
                    "  New Name  ",
                    null,
                    null,
                    "  New Location  ",
                    null,
                    null,
                    null,
                    null
            );
            assertEquals("New Name", asset.getName());
            assertEquals("New Location", asset.getLocation());
        }

        @Test
        @DisplayName("should be no-op when all fields are null")
        void updateFields_allNull_succeeds() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            asset.updateFields(null, null, null, null, null, null, null, null);
            assertEquals(AssetStatus.ACTIVE, asset.getStatus());
            assertEquals("TX-001", asset.getName());
        }

        @Test
        @DisplayName("should update installation date when provided")
        void updateFields_withInstallationDate_succeeds() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            LocalDate newDate = LocalDate.of(2023, 6, 15);
            asset.updateFields(null, newDate, null, null, null, null, null, null);
            assertEquals(newDate, asset.getInstallationDate());
        }
    }

    // ---- Status transitions via updateFields() ----

    @Nested
    @DisplayName("Status transitions via updateFields()")
    class StatusTransitionTests {

        @Test
        @DisplayName("ACTIVE -> WARNING should succeed")
        void activeToWarning_succeeds() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            asset.updateFields(null, null, AssetStatus.WARNING, null, null, null, null, null);
            assertEquals(AssetStatus.WARNING, asset.getStatus());
        }

        @Test
        @DisplayName("ACTIVE -> CRITICAL should succeed")
        void activeToCritical_succeeds() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            asset.updateFields(null, null, AssetStatus.CRITICAL, null, null, null, null, null);
            assertEquals(AssetStatus.CRITICAL, asset.getStatus());
        }

        @Test
        @DisplayName("ACTIVE -> UNDER_MAINTENANCE should succeed")
        void activeToUnderMaintenance_succeeds() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            asset.updateFields(null, null, AssetStatus.UNDER_MAINTENANCE, null, null, null, null, null);
            assertEquals(AssetStatus.UNDER_MAINTENANCE, asset.getStatus());
        }

        @Test
        @DisplayName("ACTIVE -> DECOMMISSIONED should succeed")
        void activeToDecommissioned_succeeds() {
            Asset asset = createAsset(AssetStatus.ACTIVE);
            asset.updateFields(null, null, AssetStatus.DECOMMISSIONED, null, null, null, null, null);
            assertEquals(AssetStatus.DECOMMISSIONED, asset.getStatus());
        }

        @Test
        @DisplayName("WARNING -> ACTIVE should succeed (recovery)")
        void warningToActive_succeeds() {
            Asset asset = createAsset(AssetStatus.WARNING);
            asset.updateFields(null, null, AssetStatus.ACTIVE, null, null, null, null, null);
            assertEquals(AssetStatus.ACTIVE, asset.getStatus());
        }

        @Test
        @DisplayName("CRITICAL -> ACTIVE should succeed (recovery)")
        void criticalToActive_succeeds() {
            Asset asset = createAsset(AssetStatus.CRITICAL);
            asset.updateFields(null, null, AssetStatus.ACTIVE, null, null, null, null, null);
            assertEquals(AssetStatus.ACTIVE, asset.getStatus());
        }

        @Test
        @DisplayName("UNDER_MAINTENANCE -> ACTIVE should succeed (recovery)")
        void underMaintenanceToActive_succeeds() {
            Asset asset = createAsset(AssetStatus.UNDER_MAINTENANCE);
            asset.updateFields(null, null, AssetStatus.ACTIVE, null, null, null, null, null);
            assertEquals(AssetStatus.ACTIVE, asset.getStatus());
        }

        @Test
        @DisplayName("UNDER_MAINTENANCE -> DECOMMISSIONED should succeed")
        void underMaintenanceToDecommissioned_succeeds() {
            Asset asset = createAsset(AssetStatus.UNDER_MAINTENANCE);
            asset.updateFields(null, null, AssetStatus.DECOMMISSIONED, null, null, null, null, null);
            assertEquals(AssetStatus.DECOMMISSIONED, asset.getStatus());
        }

        @Test
        @DisplayName("DECOMMISSIONED -> ACTIVE should throw (terminal state)")
        void decommissionedToActive_throws() {
            Asset asset = createAsset(AssetStatus.DECOMMISSIONED);
            assertThrows(IllegalStateException.class,
                    () -> asset.updateFields(null, null, AssetStatus.ACTIVE, null, null, null, null, null));
        }

        @Test
        @DisplayName("DECOMMISSIONED -> WARNING should throw (terminal state)")
        void decommissionedToWarning_throws() {
            Asset asset = createAsset(AssetStatus.DECOMMISSIONED);
            assertThrows(IllegalStateException.class,
                    () -> asset.updateFields(null, null, AssetStatus.WARNING, null, null, null, null, null));
        }

        @Test
        @DisplayName("DECOMMISSIONED -> UNDER_MAINTENANCE should throw (terminal state)")
        void decommissionedToUnderMaintenance_throws() {
            Asset asset = createAsset(AssetStatus.DECOMMISSIONED);
            assertThrows(IllegalStateException.class,
                    () -> asset.updateFields(null, null, AssetStatus.UNDER_MAINTENANCE, null, null, null, null, null));
        }

        @Test
        @DisplayName("CRITICAL -> WARNING should throw (not allowed)")
        void criticalToWarning_throws() {
            Asset asset = createAsset(AssetStatus.CRITICAL);
            assertThrows(IllegalStateException.class,
                    () -> asset.updateFields(null, null, AssetStatus.WARNING, null, null, null, null, null));
        }

        @Test
        @DisplayName("WARNING -> DECOMMISSIONED should throw (not directly allowed)")
        void warningToDecommissioned_throws() {
            Asset asset = createAsset(AssetStatus.WARNING);
            assertThrows(IllegalStateException.class,
                    () -> asset.updateFields(null, null, AssetStatus.DECOMMISSIONED, null, null, null, null, null));
        }
    }
}
