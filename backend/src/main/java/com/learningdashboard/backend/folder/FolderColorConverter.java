package com.learningdashboard.backend.folder;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Persists {@link FolderColor} as its palette key ("teal"), the same values the V3 backfill writes. */
@Converter
public class FolderColorConverter implements AttributeConverter<FolderColor, String> {

    @Override
    public String convertToDatabaseColumn(FolderColor color) {
        return color == null ? null : color.key();
    }

    @Override
    public FolderColor convertToEntityAttribute(String key) {
        return key == null ? null : FolderColor.fromJson(key);
    }
}
