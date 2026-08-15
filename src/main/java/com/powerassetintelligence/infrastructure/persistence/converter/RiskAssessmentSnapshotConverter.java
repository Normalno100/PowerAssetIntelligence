package com.powerassetintelligence.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerassetintelligence.core.ai.RiskAssessmentSnapshot;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that serializes {@link RiskAssessmentSnapshot} to/from JSON.
 *
 * <p>Stores the full snapshot of input features as a JSON string in a TEXT column,
 * enabling audit and reproducibility of risk assessments.</p>
 */
@Converter
public class RiskAssessmentSnapshotConverter implements AttributeConverter<RiskAssessmentSnapshot, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(RiskAssessmentSnapshot attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize RiskAssessmentSnapshot to JSON", e);
        }
    }

    @Override
    public RiskAssessmentSnapshot convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, RiskAssessmentSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize RiskAssessmentSnapshot from JSON", e);
        }
    }
}
