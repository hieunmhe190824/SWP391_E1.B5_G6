package com.carrental.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserRoleConverter implements AttributeConverter<User.UserRole, String> {

    @Override
    public String convertToDatabaseColumn(User.UserRole attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case ADMIN -> "Admin";
            case STAFF -> "Staff";
            case CUSTOMER -> "Customer";
        };
    }

    @Override
    public User.UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case "Admin" -> User.UserRole.ADMIN;
            case "Staff" -> User.UserRole.STAFF;
            case "Customer" -> User.UserRole.CUSTOMER;
            default -> throw new IllegalArgumentException("Unknown role: " + dbData);
        };
    }
}


