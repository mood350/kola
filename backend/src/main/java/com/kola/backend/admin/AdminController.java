package com.kola.backend.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAnalyticsService analyticsService;

    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        return analyticsService.overview();
    }
}
