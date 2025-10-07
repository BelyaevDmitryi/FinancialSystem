package com.fs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fs.dto.ClassesPercentDto;
import com.fs.dto.CostDto;
import com.fs.dto.ClassValue;
import com.fs.service.StatisticService;

@RequestMapping("/statistic")
@RequiredArgsConstructor
@RestController
public class StatisticController {
    private final StatisticService statisticService;

    @GetMapping("/classes/{userId}")
    public ClassesPercentDto getClassStat(@PathVariable String userId) {
        return statisticService.getStatisticOfClassesByUserId(userId);
    }

    @GetMapping("/cost/{userId}")
    public CostDto getCostPortfolio(@PathVariable String userId) {
        return statisticService.getCostPortfolio(userId);
    }

    @GetMapping("/classes/{userId}/{type}")
    public ClassValue getClassStat(@PathVariable String userId, @PathVariable String type) {
        return statisticService.getStatisticOfClassByUserId(userId, type);
    }
}
