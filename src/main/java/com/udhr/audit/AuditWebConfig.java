package com.udhr.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuditWebConfig implements WebMvcConfigurer {

    private final PatientAccessAuditInterceptor patientAccessAuditInterceptor;

    public AuditWebConfig(PatientAccessAuditInterceptor patientAccessAuditInterceptor) {
        this.patientAccessAuditInterceptor = patientAccessAuditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(patientAccessAuditInterceptor).addPathPatterns("/api/**");
    }
}
