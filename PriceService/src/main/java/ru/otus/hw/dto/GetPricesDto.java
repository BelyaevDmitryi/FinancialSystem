package ru.otus.hw.dto;

import lombok.Value;

import java.util.List;

@Value
public class GetPricesDto {
    private List<String> figies;
}
