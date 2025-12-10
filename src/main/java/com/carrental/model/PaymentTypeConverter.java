package com.carrental.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PaymentTypeConverter implements AttributeConverter<Payment.PaymentType, String> {

    @Override
    public String convertToDatabaseColumn(Payment.PaymentType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Payment.PaymentType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Payment.PaymentType.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported payment type value: " + dbData, ex);
        }
    }
}

