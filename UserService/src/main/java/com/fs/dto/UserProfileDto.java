package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {
    private Long id;
    private String username; // Логин для входа (name в БД)
    private String nickname; // Отображаемое имя (никнейм)
    private String avatarUrl;
    private List<String> roles;
}
