package com.secondstitch.stats;

import com.secondstitch.auth.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    /** GET /api/stats — savings, cost per wear and closet breakdown for your closet. */
    @GetMapping("/api/stats")
    public StatsResponse stats(@AuthenticationPrincipal Jwt jwt) {
        return service.summary(CurrentUser.id(jwt));
    }
}
