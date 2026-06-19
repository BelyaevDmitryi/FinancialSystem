package com.fs.support;

import com.fs.feignclient.BrokerIntegrationServiceClient;
import com.fs.feignclient.UserServiceClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestFeignConfig {

    @Bean
    public BrokerIntegrationServiceClient brokerIntegrationServiceClient() {
        return mock(BrokerIntegrationServiceClient.class);
    }

    @Bean
    public UserServiceClient userServiceClient() {
        return mock(UserServiceClient.class);
    }
}
