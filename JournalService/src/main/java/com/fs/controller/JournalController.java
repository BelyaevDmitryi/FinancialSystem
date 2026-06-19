package com.fs.controller;

import com.fs.domain.Position;
import com.fs.dto.PositionDto;
import com.fs.dto.TradeDto;
import com.fs.service.JournalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/journal")
public class JournalController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping("/trades")
    public List<TradeDto> getTrades(@RequestHeader(USER_ID_HEADER) Long userId) {
        return journalService.getTradesForUser(userId);
    }

    @GetMapping("/positions")
    public List<PositionDto> getPositions(@RequestHeader(USER_ID_HEADER) Long userId) {
        return journalService.getPositionsForUser(userId).stream()
                .map(this::toPositionDto)
                .toList();
    }

    @GetMapping("/positions/{figi}")
    public PositionDto getPosition(@RequestHeader(USER_ID_HEADER) Long userId,
                                   @PathVariable String figi) {
        return toPositionDto(journalService.getPositionForUser(userId, figi));
    }

    private PositionDto toPositionDto(Position position) {
        return new PositionDto(
                position.getUserId(),
                position.getFigi(),
                position.getQuantity(),
                position.getAvgPrice()
        );
    }
}
