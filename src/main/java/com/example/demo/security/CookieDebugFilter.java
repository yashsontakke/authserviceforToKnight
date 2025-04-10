//package com.example.demo.security;
//
//
//
//import jakarta.servlet.*;
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.stream.Collectors;
//
//@Component
//// Order HIGHER_PRECEDENCE means runs earlier.
//// CsrfFilter default is -100. Let's run just before it.
//@Order(-101)
//public class CookieDebugFilter implements Filter {
//
//    private static final Logger log = LoggerFactory.getLogger(CookieDebugFilter.class);
//
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
//            throws IOException, ServletException {
//
//        HttpServletRequest request = (HttpServletRequest) servletRequest;
//        Cookie[] cookies = request.getCookies(); // Get cookies as seen by the servlet API
//
//        String cookieInfo = "NONE"; // Default if no cookies array
//        if (cookies != null) {
//            cookieInfo = Arrays.stream(cookies)
//                    .map(c -> c.getName() + "=" + c.getValue() + "; HttpOnly=" + c.isHttpOnly() + "; Path=" + c.getPath())
//                    .collect(Collectors.joining(" | "));
//        }
//
//        // Log specifically for the failing request path to reduce noise
//        String path = request.getRequestURI();
//        String method = request.getMethod();
//        if (path.equals("/api/users/me/profile") && "POST".equalsIgnoreCase(method)) {
//             log.info(">>> CookieDebugFilter <<< Cookies received by Servlet API for {}: [{}]", path, cookieInfo);
//             // Also log the specific header the CsrfFilter will look for
//             String csrfHeader = request.getHeader("X-XSRF-TOKEN");
//             log.info(">>> CookieDebugFilter <<< X-XSRF-TOKEN Header for {}: [{}]", path, csrfHeader);
//        }
//
//        filterChain.doFilter(request, servletResponse); // Continue the chain
//    }
//}