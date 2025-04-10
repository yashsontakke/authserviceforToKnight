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
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

//import com.example.demo.security.CsrfCookieFilter;
import com.example.demo.security.JwtCookieAuthenticationFilter;
import com.example.demo.security.StatelessCsrfTokenRepository;

//import org.springframework.security.web.csrf.CsrfTokenRequestAttributeNameRequestHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	 @Autowired
	    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

	    @Bean
	     SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//	    	 StatelessCsrfTokenRepository tokenRepository = new StatelessCsrfTokenRepository();
	        http
	            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
	            .csrf(csrf -> csrf.disable())  
	            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//	            .addFilterBefore(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
	            .authorizeHttpRequests(auth -> auth
	                .requestMatchers("/api/auth/**" , "/actuator/prometheus" , "/error").permitAll()
	                .anyRequest().authenticated()
	            )
	            .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
	            
	            // If frameOptions was the ONLY thing inside .headers(), you might remove the .headers() call entirely
	            // unless you plan to add other security headers soon. Leaving an empty headers block is also fine.
	            // Example if removing entirely (only if nothing else was in headers):
	            // .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // End previous line with semicolon

	        return http.build();
	        
//	        http
//	        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//	        .csrf(csrf -> csrf
//	            .csrfTokenRepository(tokenRepository)
//	            .ignoringRequestMatchers("/api/auth/**")
//	        )
//	        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//	        .authorizeHttpRequests(auth -> auth
//	            .requestMatchers("/api/auth/**").permitAll()
//	            .anyRequest().authenticated()
//	        )
//	        .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//	        // Add a filter to ensure the token is in the response
//	        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

//	    return http.build();
	    }

    // Keep your CORS configuration bean
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS" , "PATCH"));
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