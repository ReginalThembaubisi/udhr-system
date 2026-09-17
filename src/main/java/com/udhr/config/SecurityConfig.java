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
                .requestMatchers("/api/auth/change-password").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/patient/me/**").hasRole("PATIENT")
                .requestMatchers("/api/symptom-checker/**").hasRole("PATIENT")
                .requestMatchers("/api/health-guidance/**").hasRole("PATIENT")
                .requestMatchers("/api/food-checker/**").hasRole("PATIENT")

                // ---- Clinical / prescribing: DOCTOR + NURSE only, never ADMIN ----
                .requestMatchers("/api/clinical-alerts/drug-food-audit", "/api/clinical-alerts/my-alerts").hasRole("PATIENT")
                // A patient's alert/adherence/symptom-check timeline is clinical detail
                // (symptom urgency, medication names, alert messages) — same as the rest
                // of clinical-alerts, no ADMIN. (Previously carved out as shared/read;
                // reclassified after the Clinical tab's own read views were locked down.)
                .requestMatchers("/api/clinical-alerts/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/reminders/patient").hasRole("PATIENT")
                .requestMatchers("/api/reminders/adherence/**").hasRole("PATIENT")
                // Reveals medication names and adherence detail per dose — clinical, no ADMIN
                // (reclassified alongside the Clinical tab's Patient Adherence History card).
                .requestMatchers("/api/reminders/patient/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/symptoms").hasAnyRole("PATIENT", "DOCTOR", "NURSE", "ADMIN")
                // Patient lookup/registration/record-view is shared/read + clerical intake, not a
                // clinical decision, so it stays open to ADMIN (consistent with Staff Management
                // being the front desk's own administrative workflow).
                .requestMatchers("/api/patients/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers("/api/allergies/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/chronic-conditions/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/diagnoses/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/prescriptions/report/**").hasRole("ADMIN")
                .requestMatchers("/api/prescriptions/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/lab-results/report/**").hasRole("ADMIN")
                .requestMatchers("/api/lab-results/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/immunizations/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/vitals/**").hasAnyRole("DOCTOR", "NURSE")
                // Discharge is the clinical decision that closes a visit out — same footing as
                // check-in below. The rest of /api/visits/** (visit history, visit creation) stays
                // open to ADMIN as a shared/read + record-keeping path.
                .requestMatchers("/api/visits/discharge").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/visits/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                // Facility creation is administrative (same as staff registration); only listing
                // facilities (for the referral-destination dropdown) is shared/read.
                .requestMatchers(HttpMethod.POST, "/api/facilities").hasRole("ADMIN")
                .requestMatchers("/api/facilities/**").hasAnyRole("DOCTOR", "NURSE", "ADMIN")

                // ---- Reception/intake: DOCTOR + NURSE only, never ADMIN or PHARMACIST ----
                // Check-in/triage and calling a patient into consultation are clinical intake
                // decisions (department, urgency, who sees the patient next) — not an admin or
                // dispensing function. The rest of /api/queue/** (today's queue, pharmacy queue,
                // complete/cancel/urgency/send-to-pharmacy) stays open to ADMIN/PHARMACIST as
                // shared/read plus routine queue housekeeping.
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

                // ---- Administrative: ADMIN only, no clinical roles ----
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
