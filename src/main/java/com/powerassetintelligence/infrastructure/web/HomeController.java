package com.powerassetintelligence.infrastructure.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "service", "Power Asset Intelligence API",
                "status", "UP",
                "docs", "/swagger-ui.html",
                "health", "/actuator/health"
        );
    }
}
