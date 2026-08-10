package com.powerassetintelligence.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerassetintelligence.core.ai.RiskFactor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/**
 * JPA AttributeConverter that serializes {@link List<RiskFactor>} to/from JSON.
 *
 * <p>Stores structured risk factors as a JSON array in a single TEXT column,
 * avoiding a separate table while keeping the data queryable.</p>
 */
@Converter
public class RiskFactorConverter implements AttributeConverter<List<RiskFactor>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<RiskFactor> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize RiskFactor list to JSON", e);
        }
    }

    @Override
    public List<RiskFactor> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, RiskFactor.class));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize RiskFactor list from JSON", e);
        }
    }
}
