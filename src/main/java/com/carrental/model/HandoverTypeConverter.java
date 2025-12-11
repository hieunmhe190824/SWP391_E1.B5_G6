package com.carrental.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class HandoverTypeConverter implements AttributeConverter<Handover.HandoverType, String> {

    @Override
    public String convertToDatabaseColumn(Handover.HandoverType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Handover.HandoverType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // Normalize to enum constant names (handles legacy values like "Pickup" or "pickup")
        return Handover.HandoverType.valueOf(dbData.toUpperCase());
    }
}

