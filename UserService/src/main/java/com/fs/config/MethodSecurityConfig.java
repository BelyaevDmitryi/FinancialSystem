package com.fs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Конфигурация для Method Security с поддержкой ACL.
 * Использует MethodSecurityExpressionHandler из AclConfig для обработки ACL разрешений.
 * В Spring Security 6.x бин MethodSecurityExpressionHandler с аннотацией @Primary
 * из AclConfig автоматически используется Spring Security.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
    // В Spring Security 6.x бин MethodSecurityExpressionHandler с @Primary из AclConfig
    // автоматически используется для method security
}
