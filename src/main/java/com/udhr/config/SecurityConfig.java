package com.udhr.config;

import com.udhr.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/patient/me/**").hasRole("PATIENT")
                .requestMatchers("/api/symptom-checker/**").hasRole("PATIENT")
                .requestMatchers("/api/health-guidance/**").hasRole("PATIENT")
                .requestMatchers("/api/food-checker/**").hasRole("PATIENT")
                .requestMatchers("/api/clinical-alerts/drug-food-audit", "/api/clinical-alerts/my-alerts").hasRole("PATIENT")
                .requestMatchers("/api/clinical-alerts/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/reminders/patient").hasRole("PATIENT")
                .requestMatchers("/api/reminders/adherence/**").hasRole("PATIENT")
                .requestMatchers("/api/reminders/patient/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/symptoms").hasAnyRole("PATIENT", "DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/patients/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/allergies/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/chronic-conditions/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/diagnoses/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/prescriptions/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/lab-results/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/visits/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/facilities/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/staff/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
