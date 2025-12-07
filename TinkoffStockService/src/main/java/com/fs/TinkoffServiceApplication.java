package com.fs;

import com.fs.config.NettyNativeDisabler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Slf4j
@EnableAsync
public class TinkoffServiceApplication {
    static {
        // Disable Netty native libraries BEFORE any other classes are loaded
        NettyNativeDisabler.disable();
    }
    
    public static void main(String[] args) {
        // Ensure Netty native libraries are disabled before Spring starts
        NettyNativeDisabler.disable();
        
        SpringApplication.run(TinkoffServiceApplication.class, args);

        log.info("Application started");
    }
}
