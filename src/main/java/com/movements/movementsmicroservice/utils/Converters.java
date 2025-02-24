package com.movements.movementsmicroservice.utils;

import com.movements.movementsmicroservice.model.Movement.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

public class Converters {

    @WritingConverter
    public static class TypeMovementWriteConverter implements Converter<TypeMovement, String> {
        @Override
        public String convert(TypeMovement source) {
            return source.name();
        }
    }

    @ReadingConverter
    public static class TypeMovementReadConverter implements Converter<String, TypeMovement> {
        @Override
        public TypeMovement convert(String source) {
            return TypeMovement.valueOf(source);
        }
    }
}
