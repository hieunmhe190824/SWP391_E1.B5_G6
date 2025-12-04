package com.carrental.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<User.UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(User.UserStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    @Override
    public User.UserStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case "Active" -> User.UserStatus.ACTIVE;
            case "Inactive" -> User.UserStatus.INACTIVE;
            default -> throw new IllegalArgumentException("Unknown status: " + dbData);
        };
    }
}


