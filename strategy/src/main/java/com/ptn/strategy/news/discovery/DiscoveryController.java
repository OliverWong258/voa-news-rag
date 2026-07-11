package com.ptn.strategy.news.discovery;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/discovery")
public class DiscoveryController {

    private final VoaDiscoveryService discoveryService;

    public DiscoveryController(VoaDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @PostMapping
    public ResponseEntity<DiscoveryResult> discover() {
        return ResponseEntity.ok(discoveryService.discover());
    }
}
