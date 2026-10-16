package com.udhr.config;

import com.udhr.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Scripts, styles and fonts only from this site, so an injected <script>
    // or inline handler can't run. Inline styles stay allowed because the
    // screens use style attributes.
    // Images may come from any HTTPS site: announcement photos are links the
    // front desk pastes in, and an image can't run code.
    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "font-src 'self' data:",
            "img-src 'self' data: blob: https:",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // A request with no valid sign-in gets 401, not Spring's default 403,
            // so the page can tell "your session expired" (refresh and retry)
            // apart from "your role isn't allowed to do this" (fix list P1-2).
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            // Browser security headers on every response, the app's own pages
            // included (fix list P1-7).
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // Spring only sends HSTS on HTTPS requests, and Render's proxy
                // reaches the app over plain HTTP, so it is always sent here.
                // Browsers ignore it on plain-HTTP pages, so localhost is unaffected.
                .httpStrictTransportSecurity(hsts -> hsts.disable())
                .addHeaderWriter(new StaticHeadersWriter("Strict-Transport-Security", "max-age=31536000; includeSubDomains")))
            .authorizeHttpRequests(auth -> auth
                // The built React app is served as static files from this same
                // service in production (see Dockerfile) — it has to be reachable
                // before login, otherwise no one can even load the page to log in.
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg", "/icons.svg", "/vite.svg").permitAll()
                // Spring renders refusals through /error as a second request
                // that carries no sign-in. Left protected, every 403 ("not
                // allowed") turned into a 401 ("not signed in") on a real server.
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/auth/change-password").authenticated()
                .requestMatchers("/api/auth/2fa/**").authenticated()
                .requestMatchers("/api/auth/**").permitAll()

                // ---- System administrator: oversight of the system itself ----
                // Account management, forced logouts and the audit trail. Kept
                // separate from the clinic's "ADMIN" front desk, and from every
                // clinical role — this person manages access, not patients.
                // Listed before the rules below so it is matched first.
                .requestMatchers("/api/system-admin/**").hasRole("SYSTEM_ADMIN")
                // The login screen fetches this before anyone has signed in.
                .requestMatchers(HttpMethod.GET, "/api/announcements/public").permitAll()
                // Public notices show on every patient's sign-in screen across the
                // network, so they belong to the system administrator, not one
                // clinic's front desk.
                .requestMatchers("/api/announcements/**").hasRole("SYSTEM_ADMIN")
                .requestMatchers("/api/patient/me/**").hasRole("PATIENT")
                .requestMatchers("/api/patient/chatbot/**").hasRole("PATIENT")
                .requestMatchers("/api/symptom-checker/**").hasRole("PATIENT")
                .requestMatchers("/api/health-guidance/**").hasRole("PATIENT")
                .requestMatchers("/api/food-checker/**").hasRole("PATIENT")
                .requestMatchers("/api/clinical-alerts/drug-food-audit", "/api/clinical-alerts/my-alerts").hasRole("PATIENT")
                .requestMatchers("/api/clinical-alerts/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/reminders/patient").hasRole("PATIENT")
                .requestMatchers("/api/reminders/adherence/**").hasRole("PATIENT")
                .requestMatchers("/api/reminders/patient/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/symptoms").hasAnyRole("PATIENT", "DOCTOR", "NURSE")
                // Registering patients is the front desk's job alone, so every
                // new file goes through the same duplicate check and consent
                // step. The clinical record stays off-limits to the front desk.
                .requestMatchers(HttpMethod.POST, "/api/patients").hasRole("ADMIN")
                // Only the doctor's screen ever reads the full clinical record (diagnoses,
                // prescriptions, allergies, vitals, labs) — nurse and pharmacist don't need it
                // and shouldn't be able to pull it even by calling the API directly.
                .requestMatchers("/api/patients/*/record").hasRole("DOCTOR")
                // Bare demographic lookup (name/DOB/contact only) — used by the nurse's
                // vitals screen. Pharmacist and admin have their own dedicated endpoints.
                .requestMatchers("/api/patients/*").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/checkin/**").hasRole("ADMIN")
                .requestMatchers("/api/diagnoses/**").hasRole("DOCTOR")
                // Management reports are for running the network, not reception.
                .requestMatchers("/api/prescriptions/report/**").hasRole("SYSTEM_ADMIN")
                .requestMatchers("/api/prescriptions/**").hasRole("DOCTOR")
                .requestMatchers("/api/lab-results/report/**").hasRole("SYSTEM_ADMIN")
                .requestMatchers("/api/lab-results/my-results").hasRole("PATIENT")
                .requestMatchers("/api/lab-results/**").hasAnyRole("DOCTOR", "NURSE")
                // Discharge is the clinical decision that closes a visit out — same footing as
                // referral creation, never PHARMACIST. The rest of /api/visits/** stays open
                // to PHARMACIST as shared/read (e.g. seeing where a patient came from).
                .requestMatchers("/api/visits/discharge").hasAnyRole("DOCTOR", "NURSE")
                // Opening a visit is a clinical intake step; the pharmacist only
                // reads visits to see where a patient came from.
                .requestMatchers(HttpMethod.POST, "/api/visits").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/visits/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST")
                // Allergies had no rule at all, so any signed-in user (a patient, the
                // front desk) could read or add them for any patient.
                .requestMatchers("/api/allergies/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/vitals/**").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/pharmacy/**").hasRole("PHARMACIST")
                .requestMatchers("/api/immunizations/**").hasAnyRole("DOCTOR", "NURSE")

                // ---- Reception/intake queue: check-in and calling a patient into consultation
                // are clinical intake decisions; the rest (today's queue, pharmacy queue,
                // complete/cancel/urgency) stays open to ADMIN/PHARMACIST as shared/read.
                .requestMatchers(HttpMethod.POST, "/api/queue/check-in").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers(HttpMethod.POST, "/api/queue/*/call").hasAnyRole("DOCTOR", "NURSE")
                // Urgency, sending to pharmacy and completing a visit are clinical
                // calls; the front desk and pharmacist used to be able to make them.
                .requestMatchers(HttpMethod.POST, "/api/queue/*/urgency", "/api/queue/*/send-to-pharmacy", "/api/queue/*/complete").hasAnyRole("DOCTOR", "NURSE")
                // The front desk may cancel a check-in when someone leaves.
                .requestMatchers(HttpMethod.POST, "/api/queue/*/cancel").hasAnyRole("DOCTOR", "NURSE", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/queue/pharmacy").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST")
                .requestMatchers(HttpMethod.GET, "/api/queue/patient/*").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers("/api/queue/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST", "ADMIN")

                // Referral creation/response is a clinical judgement call; the inbox/outgoing/report
                // views are shared/read.
                .requestMatchers("/api/referrals/report/**").hasAnyRole("ADMIN", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/referrals").hasAnyRole("DOCTOR", "NURSE")
                .requestMatchers(HttpMethod.POST, "/api/referrals/*/respond").hasAnyRole("DOCTOR", "NURSE")
                // Front desk only ever sees the report above, which leaves out the
                // clinical summary; the referrals themselves are clinical records.
                .requestMatchers("/api/referrals/**").hasAnyRole("DOCTOR", "NURSE")

                // ---- Dispensing/stock: DOCTOR + NURSE + PHARMACIST, never ADMIN ----
                .requestMatchers("/api/dispensing/report/**").hasRole("SYSTEM_ADMIN")
                .requestMatchers("/api/dispensing/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST")
                .requestMatchers("/api/stock/report/**").hasRole("SYSTEM_ADMIN")
                .requestMatchers("/api/stock/**").hasAnyRole("DOCTOR", "NURSE", "PHARMACIST")

                // Facility creation is administrative; listing facilities (needed for the
                // referral-destination dropdown) is shared/read with DOCTOR/NURSE too.
                // Adding a facility puts it in every clinician's referral list, so
                // it is the system administrator's job, not the front desk's.
                .requestMatchers(HttpMethod.POST, "/api/facilities").hasRole("SYSTEM_ADMIN")
                .requestMatchers("/api/facilities/**").hasAnyRole("DOCTOR", "NURSE", "SYSTEM_ADMIN")
                // Creating and deactivating accounts decides who can see patient
                // data, so it belongs to the system administrator alone. The front
                // desk (ADMIN) used to have this too, which let a clerk create a
                // SYSTEM_ADMIN account for themselves.
                .requestMatchers("/api/staff/**").hasRole("SYSTEM_ADMIN")
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
