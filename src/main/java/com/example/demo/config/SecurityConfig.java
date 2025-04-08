package com.example.demo.config;


import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy; // Import Policy
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Import standard filter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.security.JwtCookieAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	 @Autowired
	    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

	    @Bean
	     SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	        http
	            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
	            .csrf(csrf -> csrf
	                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
	                    // --- ADD THIS LINE to ignore CSRF for auth paths ---
	                    .ignoringRequestMatchers("/api/auth/**")
	                    // ---------------------------------------------------
	             )
	            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	            .authorizeHttpRequests(auth -> auth
	                .requestMatchers("/api/auth/**").permitAll()
	                .anyRequest().authenticated()
	            )
	            .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
	            
	            // If frameOptions was the ONLY thing inside .headers(), you might remove the .headers() call entirely
	            // unless you plan to add other security headers soon. Leaving an empty headers block is also fine.
	            // Example if removing entirely (only if nothing else was in headers):
	            // .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // End previous line with semicolon

	        return http.build();
	    }

    // Keep your CORS configuration bean
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*")); // Allow all headers, including Authorization
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // You might need PasswordEncoder bean if setting up other auth types,
    // but not strictly required just for JWT validation if passwords aren't checked.
    // @Bean
    // public PasswordEncoder passwordEncoder() {
    //     return new BCryptPasswordEncoder();
    // }
}