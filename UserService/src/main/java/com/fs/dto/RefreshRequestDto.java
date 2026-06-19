package com.fs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос обмена refresh JWT на новую пару токенов")
public class RefreshRequestDto {

    @NotBlank(message = "Refresh token обязателен")
    @Schema(
            description = "Refresh JWT, полученный при signin (claim type=refresh)",
            example = "eyJhbGciOiJIUzI1NiJ9..."
    )
    private String refreshToken;
}
