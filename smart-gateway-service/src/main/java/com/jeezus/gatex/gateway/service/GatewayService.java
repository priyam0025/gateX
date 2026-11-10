package com.jeezus.gatex.gateway.service;

import com.jeezus.gatex.gateway.model.CompiledRoute;
import com.jeezus.gatex.gateway.dto.GatewayErrorResponse;
import com.jeezus.gatex.gateway.dto.GatewayRoute;
import com.jeezus.gatex.gateway.model.RateLimitResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class GatewayService {
    private final RouteRegistry routeRegistry;
    private final ObjectMapper objectMapper;
    private final RateLimitingService rateLimitingService;

    public GatewayService(RouteRegistry routeRegistry, ObjectMapper objectMapper) {
        this.routeRegistry = routeRegistry;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<byte[]> handle(HttpServletRequest request) {
        //1. Compiled route for incoming http request
        //2. To find rate limiting rules and apply it
        //3. Idempotency check(for POST request obviously)
        //4. Header rules
        //5. Redirect our endpoint to target
        //6. Header rules on response
        //7. Store response if idempotency was enabled
        //8. Return result to client

        long startedAt = System.nanoTime();
        String outcome = "TARGET_ERROR";
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        boolean rateLimited = false;
        HttpHeaders policyHeaders = new HttpHeaders();

        String correlationId = correlationId(request);

        Optional<CompiledRoute> matchesRoute = routeRegistry.find(request.getRequestURI());
        if (matchesRoute.isEmpty()) {
            outcome = "ROUTE_NOT_FOUND";
            statusCode = HttpStatus.NOT_FOUND.value();
            return error(HttpStatus.NOT_FOUND, "No active route matches with the requsted route",
                    correlationId, null);
        }

        CompiledRoute compiledRoute = matchesRoute.get(); // get() gives the object from optional
        GatewayRoute route = compiledRoute.getGatewayRoute();

        if (!route.getAllowedMethods().contains(request.getMethod())) {
            outcome = "METHOD_NOT_ALLOWED";
            statusCode = HttpStatus.METHOD_NOT_ALLOWED.value();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ALLOW, String.join(", ",route.getAllowedMethods()));

            return error(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not allowed for this route",
                    correlationId, headers);
        }

        // Checking rate limiting
        RateLimitResult rateLimit = rateLimitingService.check(route, request.getRemoteAddr());
        policyHeaders = rateLimitHeaders(rateLimit);

        if (!rateLimit.isAllowed()) {
            rateLimited = true;
            outcome = "RATE_LIMITED";
            statusCode = HttpStatus.TOO_MANY_REQUESTS.value();

            policyHeaders.set(HttpHeaders.RETRY_AFTER,
                    String.valueOf(rateLimit.getRetryAfterSeconds()));

            return error(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded",
                    correlationId, policyHeaders);
        }

        // Idempotency check
        if (route.isIdempotencyEnabled() && "POST".equals(request.getMethod())) {
            
        }

        return null;
    }

    private String correlationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private HttpHeaders rateLimitHeaders(RateLimitResult result) {
        HttpHeaders headers = new HttpHeaders();
        if (result.getLimit() > 0) {
            headers.set("X-RateLimit-Limit", String.valueOf(result.getLimit()));
            headers.set("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        }
        return headers;
    }

    private ResponseEntity<byte[]> error(HttpStatus status,
                                         String message,
                                         String correlationId,
                                         HttpHeaders additionalHeaders) {
        GatewayErrorResponse response = new GatewayErrorResponse(status.value(), message,
                correlationId, LocalDateTime.now());
        HttpHeaders headers = additionalHeaders == null ? new HttpHeaders() : additionalHeaders;
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-ID", correlationId);

        return ResponseEntity.status(status).header(String.valueOf(headers))
                .body(objectMapper.writeValueAsString(response).getBytes(StandardCharsets.UTF_8));
    }
}
