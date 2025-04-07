package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
     WebClient.Builder webClientBuilder() {
        // You can customize the builder with default settings here
        // if needed across multiple WebClient instances.
        // For example: timeouts, default headers, etc.
        // HttpClient httpClient = HttpClient.create()
        //         .responseTimeout(Duration.ofSeconds(10)); // Example timeout
        // return WebClient.builder()
        //         .clientConnector(new ReactorClientHttpConnector(httpClient))
        //         .defaultHeader(HttpHeaders.USER_AGENT, "MyAppClient");

        // Return default builder if no common customization is needed yet
        return WebClient.builder();
    }

    // Optional: Create a pre-configured WebClient instance if you like
    // This isn't strictly necessary as you can inject the Builder
    @Bean
     WebClient defaultWebClient(WebClient.Builder builder) {
        // Create a general-purpose WebClient instance from the builder
        return builder.build();
    }
}