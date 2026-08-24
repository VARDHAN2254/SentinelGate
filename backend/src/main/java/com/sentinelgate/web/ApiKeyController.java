package com.sentinelgate.web;

import com.sentinelgate.dto.ApiKeyDto;
import com.sentinelgate.dto.CreateApiKeyRequest;
import com.sentinelgate.dto.CreateApiKeyResponse;
import com.sentinelgate.service.ApiKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public Mono<ResponseEntity<List<ApiKeyDto>>> getAllApiKeys() {
        return Mono.fromCallable(apiKeyService::getAllApiKeys)
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createApiKey(@RequestBody CreateApiKeyRequest request,
                                                     @RequestHeader(name = "X-User-Name", required = false) String username) {
        return Mono.fromCallable(() -> apiKeyService.generateApiKey(request, username))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).<Object>body(response))
                .onErrorResume(IllegalArgumentException.class, ex ->
                        Mono.just(ResponseEntity.badRequest().<Object>body(Map.of("error", ex.getMessage()))));
    }

    @PostMapping("/{id}/revoke")
    public Mono<ResponseEntity<Object>> revokeApiKey(@PathVariable Long id) {
        return Mono.fromCallable(() -> apiKeyService.revokeApiKey(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(revoked -> ResponseEntity.ok().<Object>body(revoked))
                .onErrorResume(IllegalArgumentException.class, ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .<Object>body(Map.of("error", ex.getMessage()))));
    }
}
