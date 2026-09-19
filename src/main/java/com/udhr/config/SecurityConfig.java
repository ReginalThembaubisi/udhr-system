package com.udhr.config;

import com.udhr.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                // The built React app is served as static files from this same
                // service in production (see Dockerfile) — it has to be reachable
                // before login, otherwise no one can even load the page to log in.
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg", "/icons.svg", "/vite.svg").permitAll()
                .requestMatchers("/api/auth/change-password").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/patient/me/**").hasRole("PATIENT")
                .requestMatchers("/api/symptom-checker/**").hasRole("PATIENT")
                .requestMatchers("/api/health-guidance/**").hasRole("PATIENT")
                .requestMatchers("/api/food-checker/**").hasRole("PATIENT")
                .requestMatchers("/api/clinical-alerts/drug-food-audit", "/api/clinical-alerts/my-alerts").hasRole("PATIENT")
                .requestMatchers("/api/clinical-alerts/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/reminders/patient").hasRole("PATIENT")
                .requestMatchers("/api/reminders/adherence/**").hasRole("PATIENT")
                .requestMatchers("/api/reminders/patient/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/symptoms").hasAnyRole("PATIENT", "DOCTOR", "NURSE")
                // Front desk (Admin) can register a new patient and check patients in,
                // but the clinical record stays off-limits to Admin.
                .requestMatchers(HttpMethod.POST, "/api/patients").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                // Only the doctor's screen ever reads the full clinical record (diagnoses,
                // prescriptions, allergies, vitals, labs) — nurse and pharmacist don't need it
                // and shouldn't be able to pull it even by calling the API directly.
                .requestMatchers("/api/patients/*/record").hasRole("DOCTOR")
                // Bare demographic lookup (name/DOB/contact only) — used by the nurse's
                // vitals screen. Pharmacist and admin have their own dedicated endpoints.
                .requestMatchers("/api/patients/*").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/checkin/**").hasRole("ADMIN")
                .requestMatchers("/api/diagnoses/**").hasRole("DOCTOR")
                .requestMatchers("/api/prescriptions/**").hasRole("DOCTOR")
                .requestMatchers("/api/lab-results/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/visits/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST")
                .requestMatchers("/api/vitals/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/pharmacy/**").hasRole("PHARMACIST")
                .requestMatchers("/api/immunizations/**").hasAnyRole("DOCTOR", "NURSE")

                // ---- Reception/intake queue: check-in and calling a patient into consultation
                // are clinical intake decisions; the rest (today's queue, pharmacy queue,
                // complete/cancel/urgency) stays open to ADMIN/PHARMACIST as shared/read.
                .requestMatchers(HttpMethod.POST, "/api/queue/check-in").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers(HttpMethod.POST, "/api/queue/*/call").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/queue/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST", "ADMIN")

                // Referral creation/response is a clinical judgement call; the inbox/outgoing/report
                // views are shared/read.
                .requestMatchers("/api/referrals/report/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/referrals").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers(HttpMethod.POST, "/api/referrals/*/respond").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/referrals/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")

                // ---- Dispensing/stock: DOCTOR + NURSE + PHARMACIST, never ADMIN ----
                .requestMatchers("/api/dispensing/report/**").hasRole("ADMIN")
                .requestMatchers("/api/dispensing/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST")
                .requestMatchers("/api/stock/report/**").hasRole("ADMIN")
                .requestMatchers("/api/stock/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST")

                // Facility creation is administrative; listing facilities (needed for the
                // referral-destination dropdown) is shared/read with DOCTOR/NURSE too.
                .requestMatchers(HttpMethod.POST, "/api/facilities").hasRole("ADMIN")
                .requestMatchers("/api/facilities/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
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
