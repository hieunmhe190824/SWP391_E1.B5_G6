package com.carrental.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PaymentMethodConverter implements AttributeConverter<Payment.PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(Payment.PaymentMethod attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Payment.PaymentMethod convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Payment.PaymentMethod.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported payment method value: " + dbData, ex);
        }
    }
}

