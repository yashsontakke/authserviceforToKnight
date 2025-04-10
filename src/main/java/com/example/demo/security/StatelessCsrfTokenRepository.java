package com.example.demo.security;

import java.util.UUID;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class StatelessCsrfTokenRepository implements CsrfTokenRepository {
    private static final String DEFAULT_CSRF_PARAMETER_NAME = "_csrf";
    private static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String DEFAULT_CSRF_COOKIE_NAME = "XSRF-TOKEN";
    
    private final String parameterName;
    private final String headerName;
    private final String cookieName;
    
    public StatelessCsrfTokenRepository() {
        this.parameterName = DEFAULT_CSRF_PARAMETER_NAME;
        this.headerName = DEFAULT_CSRF_HEADER_NAME;
        this.cookieName = DEFAULT_CSRF_COOKIE_NAME;
    }
    
    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        // For stateless apps, we verify the token matches the cookie
        // rather than a server-side stored value
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        
        // If no token in cookie, generate a new one
        if (token == null) {
            token = UUID.randomUUID().toString();
        }
        
        return new DefaultCsrfToken(headerName, parameterName, token);
    }
    
    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        String tokenValue = token != null ? token.getToken() : "";
        
        // Delete cookie if token is empty
        if (tokenValue.isEmpty()) {
            Cookie cookie = new Cookie(cookieName, "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            cookie.setHttpOnly(false);
            response.addCookie(cookie);
        } else {
            // Add or update the cookie
            Cookie cookie = new Cookie(cookieName, tokenValue);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 1 day
            cookie.setHttpOnly(false);
            response.addCookie(cookie);
        }
    }
    
    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        // For stateless apps, get the token from the cookie directly
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return new DefaultCsrfToken(headerName, parameterName, cookie.getValue());
                }
            }
        }
        return null;
    }
}