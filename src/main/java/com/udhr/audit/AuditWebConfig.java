package com.udhr.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuditWebConfig implements WebMvcConfigurer {

    private final PatientAccessAuditInterceptor patientAccessAuditInterceptor;
    private final ReportExportAuditInterceptor reportExportAuditInterceptor;
    private final AccessReasonInterceptor accessReasonInterceptor;

    public AuditWebConfig(PatientAccessAuditInterceptor patientAccessAuditInterceptor,
                          ReportExportAuditInterceptor reportExportAuditInterceptor,
                          AccessReasonInterceptor accessReasonInterceptor) {
        this.patientAccessAuditInterceptor = patientAccessAuditInterceptor;
        this.reportExportAuditInterceptor = reportExportAuditInterceptor;
        this.accessReasonInterceptor = accessReasonInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // First: a record outside today's list needs a reason before anything is read.
        registry.addInterceptor(accessReasonInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(patientAccessAuditInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(reportExportAuditInterceptor).addPathPatterns("/api/**");
    }
}
