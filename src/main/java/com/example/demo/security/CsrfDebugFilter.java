//package com.example.demo.security;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.web.csrf.CsrfToken;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import io.jsonwebtoken.io.IOException;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE + 10)  // Set an appropriate order to run before the CSRF filter
//public class CsrfDebugFilter extends OncePerRequestFilter {
//    
//    private static final Logger logger = LoggerFactory.getLogger(CsrfDebugFilter.class);
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
//            throws ServletException, IOException, java.io.IOException {
//        
//        // Log all cookies
//        if (request.getCookies() != null) {
//            logger.info("All cookies:");
//            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
//                logger.info("Cookie: {} = {}, Path = {}", cookie.getName(), cookie.getValue(), cookie.getPath());
//            }
//        } else {
//            logger.info("No cookies found in request");
//        }
//        
//        // Log CSRF token from header
//        String csrfHeader = request.getHeader("X-XSRF-TOKEN");
//        logger.info("X-XSRF-TOKEN header: {}", csrfHeader);
//        
//        // Get the expected token from the CSRF token repository
//        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
//        if (csrfToken != null) {
//            logger.info("Expected CSRF token: {}", csrfToken.getToken());
//            logger.info("Expected header name: {}", csrfToken.getHeaderName());
//            logger.info("Expected parameter name: {}", csrfToken.getParameterName());
//        } else {
//            logger.info("No CsrfToken found in request attributes");
//        }
//        
//        filterChain.doFilter(request, response);
//    }
//}