package com.fs.controller;

import com.fs.dto.FillDto;
import com.fs.dto.TradeDto;
import com.fs.service.JournalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/journal")
public class FillIngestController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final JournalService journalService;

    public FillIngestController(JournalService journalService) {
        this.journalService = journalService;
    }

    @PostMapping("/fills")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('INTERNAL')")
    public TradeDto recordFill(@RequestHeader(USER_ID_HEADER) Long userId,
                               @Valid @RequestBody FillDto fill) {
        return journalService.recordFill(fill.withUserId(userId));
    }
}
