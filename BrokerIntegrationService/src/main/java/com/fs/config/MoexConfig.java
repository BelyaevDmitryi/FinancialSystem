package com.fs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "broker.moex")
public class MoexConfig {

    private String baseUrl = "https://iss.moex.com";

    private String board = "TQBR";
}
