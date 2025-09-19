package com.haekitchenapp.recipeapp.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

final class JsonSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private JsonSupport() {
    }

    static String write(Object value) {
        OBJECT_MAPPER.writeValueAsString(value)
    }
}
