package com.tech.feedback_platform.controller;

import com.tech.feedback_platform.dto.WeeklyReportResponse;
import com.tech.feedback_platform.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/semanal")
    public WeeklyReportResponse gerarRelatorio() {
        return reportService.gerarRelatorio();
    }
}
