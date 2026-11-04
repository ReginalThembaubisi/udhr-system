package com.udhr.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuditWebConfig implements WebMvcConfigurer {

    private final PatientAccessAuditInterceptor patientAccessAuditInterceptor;
    private final ReportExportAuditInterceptor reportExportAuditInterceptor;

    public AuditWebConfig(PatientAccessAuditInterceptor patientAccessAuditInterceptor,
                          ReportExportAuditInterceptor reportExportAuditInterceptor) {
        this.patientAccessAuditInterceptor = patientAccessAuditInterceptor;
        this.reportExportAuditInterceptor = reportExportAuditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(patientAccessAuditInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(reportExportAuditInterceptor).addPathPatterns("/api/**");
    }
}
