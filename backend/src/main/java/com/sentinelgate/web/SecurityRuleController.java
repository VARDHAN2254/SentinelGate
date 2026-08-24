package com.sentinelgate.web;

import com.sentinelgate.dto.SecurityRuleDto;
import com.sentinelgate.service.SecurityRuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/security-rules")
public class SecurityRuleController {

    private final SecurityRuleService securityRuleService;

    public SecurityRuleController(SecurityRuleService securityRuleService) {
        this.securityRuleService = securityRuleService;
    }

    @GetMapping
    public Mono<ResponseEntity<List<SecurityRuleDto>>> getAllRules() {
        return Mono.fromCallable(securityRuleService::getAllRules)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createRule(@RequestBody SecurityRuleDto dto,
                                                  @RequestHeader(name = "X-User-Name", required = false) String username) {
        return Mono.fromCallable(() -> securityRuleService.createRule(dto, username))
                .map(rule -> ResponseEntity.status(HttpStatus.CREATED).<Object>body(rule))
                .onErrorResume(IllegalArgumentException.class, ex -> 
                        Mono.just(ResponseEntity.badRequest().<Object>body(Map.of("error", ex.getMessage()))));
    }
}
