package com.urbanvogue.api_gateway.filter;

import com.urbanvogue.api_gateway.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public AuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // 1. Skip validation for Auth Service routes
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract Authorization Header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("UNAUTHORIZED: Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 3. Validate cryptographic signature
            jwtUtil.validateToken(token);

            // 4. Extract Identity
            String userEmail = jwtUtil.extractUserEmail(token);
            String userId = jwtUtil.extractUserId(token);
            String userRole = jwtUtil.extractUserRole(token);

            // 5. Mutate Request to securely pass identity in Headers downward
            HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(request);
            if (userEmail != null) requestWrapper.addHeader("X-User-Email", userEmail);
            if (userId != null) requestWrapper.addHeader("X-User-Id", userId);
            if (userRole != null) requestWrapper.addHeader("X-User-Role", userRole);

            filterChain.doFilter(requestWrapper, response);

        } catch (Exception e) {
            System.err.println("JWT Validation Failed: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("UNAUTHORIZED: Invalid JWT Token. Exact Reason: " + e.getMessage());
        }
    }

    /**
     * Mutates standard HttpServletRequest to dynamically inject new safe Headers
     */
    private static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private Map<String, String> headerMap = new ConcurrentHashMap<>();

        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            headerMap.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = super.getHeader(name);
            if (headerMap.containsKey(name)) {
                headerValue = headerMap.get(name);
            }
            return headerValue;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.addAll(headerMap.keySet());
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            List<String> values = Collections.list(super.getHeaders(name));
            if (headerMap.containsKey(name)) {
                values.add(headerMap.get(name));
            }
            return Collections.enumeration(values);
        }
    }
}
