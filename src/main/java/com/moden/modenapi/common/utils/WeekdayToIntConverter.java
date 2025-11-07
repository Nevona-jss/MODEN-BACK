package com.moden.modenapi.common.utils;

import com.moden.modenapi.common.enums.Weekday;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class WeekdayToIntConverter implements AttributeConverter<Weekday, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Weekday attribute) {
        return (attribute == null) ? null : attribute.getCode(); // 0..6
    }

    @Override
    public Weekday convertToEntityAttribute(Integer dbData) {
        return (dbData == null) ? null : Weekday.fromCode(dbData);
    }
}
