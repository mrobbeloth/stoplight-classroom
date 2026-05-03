package com.stoplight.classroom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, RateEntry> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!"/api/auth/login".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        String ip = request.getRemoteAddr();
        RateEntry entry = attempts.compute(ip, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v.windowStart > WINDOW_MS) {
                return new RateEntry(now, new AtomicInteger(1));
            }
            v.count.incrementAndGet();
            return v;
        });
        if (entry.count.get() > MAX_ATTEMPTS) {
            response.setStatus(429);
            response.getWriter().write("{\"error\":\"Too many login attempts. Try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static class RateEntry {
        final long windowStart;
        final AtomicInteger count;
        RateEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
