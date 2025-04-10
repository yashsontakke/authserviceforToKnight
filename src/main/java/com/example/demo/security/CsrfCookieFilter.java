//package com.example.demo.security;
//
//import org.springframework.security.web.csrf.CsrfToken;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import io.jsonwebtoken.io.IOException;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//public class CsrfCookieFilter extends OncePerRequestFilter {
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException, java.io.IOException {
//        
//        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
//        if (csrfToken != null) {
//            // Force the token to be included in the response
//            // This ensures the token is available in subsequent requests
//            csrfToken.getToken();
//        }
//        
//        filterChain.doFilter(request, response);
//    }
//}