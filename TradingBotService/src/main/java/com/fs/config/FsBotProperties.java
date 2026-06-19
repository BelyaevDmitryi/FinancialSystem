package com.fs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "fs.bot")
public class FsBotProperties {

    private boolean defaultPaper = true;
}
