package com.carrental.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ContractStatusConverter implements AttributeConverter<Contract.ContractStatus, String> {

    @Override
    public String convertToDatabaseColumn(Contract.ContractStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case PENDING_PAYMENT -> "Pending_Payment";
            case ACTIVE -> "Active";
            case BILL_PENDING -> "Bill_Pending";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }

    @Override
    public Contract.ContractStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case "Pending_Payment" -> Contract.ContractStatus.PENDING_PAYMENT;
            case "Active" -> Contract.ContractStatus.ACTIVE;
            case "Bill_Pending" -> Contract.ContractStatus.BILL_PENDING;
            case "Completed" -> Contract.ContractStatus.COMPLETED;
            case "Cancelled" -> Contract.ContractStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unknown contract status: " + dbData);
        };
    }
}

