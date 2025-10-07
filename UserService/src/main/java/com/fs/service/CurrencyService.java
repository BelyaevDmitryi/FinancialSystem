package com.fs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fs.config.ApiConfig;
import com.fs.domain.Currency;
import com.fs.dto.CurrencyRate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class CurrencyService {
    private final ApiConfig config;
    private final RestTemplate restTemplate;

    public CurrencyService(RestTemplateBuilder restTemplateBuilder, ApiConfig config) {
        this.config = config;
        this.restTemplate = restTemplateBuilder.build();
    }

    @Async
    public CompletableFuture<CurrencyRate> exchangeRate(Currency currency) {
        if(currency.equals(Currency.RUB)) {
            return CompletableFuture.completedFuture(new CurrencyRate("RUB", BigDecimal.valueOf(1.0)));
        }
        String url = config.getCurrencyConfig().getCurrencyService() + config.getCurrencyConfig().getGetRate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        log.info("Getting rate {} from CurrencyService", currency);
        CurrencyRate responseEntity = restTemplate.getForObject(url, CurrencyRate.class, currency);
        return CompletableFuture.completedFuture(responseEntity);
    }

    public Map<Currency, BigDecimal> getRates(List<Currency> currencyList) {
        List<CompletableFuture<CurrencyRate>> cfRates = new ArrayList<>();
        Map<Currency, BigDecimal> rates = new HashMap<>();

        currencyList.forEach(c -> cfRates.add(exchangeRate(c)));
        cfRates.stream()
                .map(CompletableFuture::join)
                .forEach(cr -> rates.put(Currency.valueOf(cr.getCharCode()), cr.getValue()));
        return rates;
    }

}
