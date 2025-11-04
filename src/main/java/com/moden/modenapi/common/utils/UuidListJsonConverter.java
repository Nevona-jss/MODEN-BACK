package com.moden.modenapi.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Converter(autoApply = false)
public class UuidListJsonConverter implements AttributeConverter<List<UUID>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<UUID>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<UUID> attribute) {
        try {
            if (attribute == null || attribute.isEmpty()) return "[]";
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize UUID list", e);
        }
    }

    @Override
    public List<UUID> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) return new ArrayList<>();
            return MAPPER.readValue(dbData, TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize UUID list", e);
        }
    }
}
