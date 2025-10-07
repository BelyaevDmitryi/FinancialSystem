package com.fs.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fs.config.ApiConfig;
import com.fs.domain.Stock;
import com.fs.dto.StocksDto;
import com.fs.exception.PriceServiceException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PriceService {
    private final ApiConfig config;

    private final RestTemplate restTemplate;

    public PriceService(RestTemplateBuilder restTemplateBuilder, ApiConfig config) {
        this.config = config;
        this.restTemplate = restTemplateBuilder.build();
    }

    public Map<String, BigDecimal> getPricesByFigies(List<Stock> stocks) {
        Map<String, BigDecimal> prices = new HashMap<>();
        String url = config.getPriceServiceConfig().getPriceService() + config.getPriceServiceConfig().getGetPricesByFigies();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<StocksDto> entity = new HttpEntity<>(new StocksDto(stocks), headers);

        ResponseEntity<StocksDto> responseEntity
                = this.restTemplate.postForEntity(url, entity, StocksDto.class);

        if(responseEntity.getStatusCode() == HttpStatus.OK) {
            StocksDto body = responseEntity.getBody();
            if (body != null && body.getStocks() != null) {
                body.getStocks().forEach(i -> prices.put(i.getFigi(), i.getPrice()));
            }
            return prices;
        } else {
            throw new PriceServiceException(responseEntity.toString());
        }
    }
}
