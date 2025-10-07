package ru.otus.hw.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
@AllArgsConstructor
public class ClassesPercentDto {
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("classes")
    private List<ClassPercentValue> classValues;
}
