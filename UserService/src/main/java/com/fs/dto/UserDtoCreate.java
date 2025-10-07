package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Value;
import com.fs.domain.Position;

import java.util.Set;

@Value
@AllArgsConstructor
public class UserDtoCreate {
    private String id;
    private String name;
    private Set<Position> portfolio;
}
