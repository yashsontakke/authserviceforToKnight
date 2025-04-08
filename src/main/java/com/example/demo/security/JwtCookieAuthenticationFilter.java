package com.example.demo.security; // Adjust package



import java.io.IOException;
import java.util.Arrays; // For Arrays.stream

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Import Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.service.JwtTokenProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie; // Import Cookie
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter { // Renamed for clarity

    private static final Logger log = LoggerFactory.getLogger(JwtCookieAuthenticationFilter.class);

    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private CustomUserDetailsService customUserDetailsService;

    // Inject cookie name from properties
    @Value("${app.jwt.cookie-name}")
    private String jwtCookieName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // --- Get JWT from Cookie ---
            String jwt = getJwtFromCookie(request);
            // --------------------------

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String userIdString = tokenProvider.getUserIdFromJWT(jwt);
                log.debug("Extracted User ID String from JWT Cookie: {}", userIdString);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(userIdString);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set authentication in context for user identified by: {}", userIdString);
            } else {
                 if (StringUtils.hasText(jwt)) log.warn("JWT Cookie validation failed.");
                 // else log.trace("No JWT cookie found."); // Can be noisy
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication from JWT cookie", ex);
        }

        filterChain.doFilter(request, response);
    }

    // --- Helper method to extract JWT from cookies ---
    private String getJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> jwtCookieName.equals(cookie.getName())) // Find cookie by name
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}