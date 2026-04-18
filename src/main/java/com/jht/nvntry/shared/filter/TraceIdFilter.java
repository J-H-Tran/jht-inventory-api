package com.jht.nvntry.shared.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Populates MDC with a traceId for the duration of every HTTP request.
 *
 * Accepts an incoming X-Trace-Id header so callers can propagate their own
 * trace context (e.g. an API gateway or upstream service). Falls back to a
 * fresh UUID when the header is absent.
 *
 * MDC is cleared in the finally block — critical for thread pool correctness.
 * Servlet containers reuse threads; a missing clear leaks the previous
 * request's traceId into unrelated log lines.
 *
 * The traceId is also written to the response as X-Trace-Id so clients can
 * correlate their logs with server logs without round-tripping to the server.
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    public static final String TRACE_ID_KEY = "traceId";
    public static final String METHOD_KEY = "httpMethod";
    public static final String PATH_KEY = "path";
    public static final String STATUS_KEY = "status";

    private final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String traceId = resolveTraceId(request);
        long start = System.currentTimeMillis();

        try {
            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(METHOD_KEY, request.getMethod());
            MDC.put(PATH_KEY, request.getRequestURI());

            response.setHeader(TRACE_ID_HEADER, traceId);
            log.info("request.start");

            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String outcome =
                    (status >= 500) ? "SERVER_ERROR" :
                    (status >= 400) ? "CLIENT_ERROR" :
                    "SUCCESS";

            MDC.put(STATUS_KEY, String.valueOf(response.getStatus()));
            if (duration > 500) {
                log.warn("request.slow durationMs={}", duration);
            }
            log.info("request.end durationMs={} outcome={}", duration, outcome);
            MDC.clear(); // never skip this - thread pool leakage
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String incoming = request.getHeader(TRACE_ID_HEADER);
        // Accept caller-supplied trace IDs (API gateway, upstream service, tests).
        // Basic length guard prevents a crafted header from polluting log lines.
        if (incoming != null && !incoming.isBlank() && incoming.length() <= 64) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}