package com.stoplight.classroom.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Per-IP rate limiter for sensitive auth endpoints, backed by Bucket4j token buckets.
 *
 * <p>Buckets are kept in memory keyed by {@code endpoint + IP}. That is fine for the
 * single-task ECS deployment used today; if the service ever scales horizontally, swap
 * the in-memory map for a distributed backend (Redis, JCache, etc.).</p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String SIGNUP_PATH = "/api/auth/teacher/signup";

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Bandwidth> policies;

    public RateLimitFilter(
            @Value("${ratelimit.login.capacity:10}") int loginCapacity,
            @Value("${ratelimit.login.window-seconds:60}") long loginWindowSeconds,
            @Value("${ratelimit.signup.capacity:3}") int signupCapacity,
            @Value("${ratelimit.signup.window-seconds:3600}") long signupWindowSeconds) {
        this.policies = Map.of(
                LOGIN_PATH, Bandwidth.builder()
                        .capacity(loginCapacity)
                        .refillIntervally(loginCapacity, Duration.ofSeconds(loginWindowSeconds))
                        .build(),
                SIGNUP_PATH, Bandwidth.builder()
                        .capacity(signupCapacity)
                        .refillIntervally(signupCapacity, Duration.ofSeconds(signupWindowSeconds))
                        .build());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        Bandwidth policy = policies.get(request.getRequestURI());
        if (policy == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getRequestURI() + "|" + clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(policy).build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(Math.max(retryAfterSeconds, 1)));
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Too many requests. Try again in "
                        + Math.max(retryAfterSeconds, 1) + " seconds.\"}");
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return Objects.requireNonNullElse(request.getRemoteAddr(), "unknown");
    }
}
