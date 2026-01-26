package com.fs.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileDto {
    @Size(min = 3, max = 50, message = "Логин должен быть от 3 до 50 символов")
    private String username; // Логин для входа
    
    @Size(min = 3, max = 50, message = "Никнейм должен быть от 3 до 50 символов")
    private String nickname; // Отображаемое имя
    
    @Size(max = 500, message = "URL аватара не должен превышать 500 символов")
    private String avatarUrl;
}
