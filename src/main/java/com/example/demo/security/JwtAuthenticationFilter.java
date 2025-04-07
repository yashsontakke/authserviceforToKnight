package com.example.demo.security;



import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component // Register as a Spring component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

 // Inside JwtAuthenticationFilter.java

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // --- CORRECTED PART ---
                // 1. Get the User ID (subject) from token as String
                String userIdString = tokenProvider.getUserIdFromJWT(jwt);
                // --- End of Correction ---

                // Log the extracted string ID
                log.debug("Extracted User ID String from JWT: {}", userIdString);

                // 2. Load user details using the String ID
                //    (The CustomUserDetailsService will parse it to Long internally)
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(userIdString);

                // 3. Create authentication object
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, // Principal
                        null,       // Credentials
                        userDetails.getAuthorities()); // Authorities

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 4. Set authentication in Spring Security's context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set authentication in context for user identified by: {}", userIdString); // Log the string ID

            } else {
                 // Only log if a token was actually present but invalid, or simply not present
                 if (StringUtils.hasText(jwt)) {
                      log.warn("JWT Token validation failed, authentication not set.");
                 } else {
                      log.trace("No JWT token found in request, authentication not set."); // Trace level might be better
                 }
            }
        } catch (Exception ex) {
            // Log any exception occurring during the authentication process
            log.error("Could not set user authentication in security context", ex);
            // Important: Do not throw ServletException here unless it's fatal,
            // allow the filter chain to proceed so other security mechanisms can potentially act.
        }

        // Continue the filter chain regardless of authentication success/failure in this filter
        filterChain.doFilter(request, response);
    }

    // Helper method getJwtFromRequest remains the same
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

   
}