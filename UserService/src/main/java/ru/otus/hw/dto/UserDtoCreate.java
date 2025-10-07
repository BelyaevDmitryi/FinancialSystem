package ru.otus.hw.dto;

import lombok.AllArgsConstructor;
import lombok.Value;
import ru.otus.hw.domain.Position;

import java.util.Set;

@Value
@AllArgsConstructor
public class UserDtoCreate {
    private String id;
    private String name;
    private Set<Position> portfolio;
}
