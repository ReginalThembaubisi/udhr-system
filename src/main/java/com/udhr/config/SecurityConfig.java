package com.udhr.config;

import com.udhr.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/change-password").authenticated()
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
                .requestMatchers("/api/diagnoses/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/prescriptions/report").hasRole("ADMIN")
                .requestMatchers("/api/prescriptions/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/lab-results/report").hasRole("ADMIN")
                .requestMatchers("/api/lab-results/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/visits/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/facilities/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/immunizations/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/queue/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/vitals/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/referrals/report").hasRole("ADMIN")
                .requestMatchers("/api/referrals/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/dispensing/report").hasRole("ADMIN")
                .requestMatchers("/api/dispensing/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST", "ADMIN")
                .requestMatchers("/api/stock/report").hasRole("ADMIN")
                .requestMatchers("/api/stock/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST", "ADMIN")
                .requestMatchers("/api/staff/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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
