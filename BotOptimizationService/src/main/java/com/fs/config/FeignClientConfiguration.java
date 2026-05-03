package com.fs.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientConfiguration {

    @Bean
    public RequestInterceptor forwardAuthorizationHeader() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(auth)) {
                template.header(HttpHeaders.AUTHORIZATION, auth);
            }
        };
    }
}
