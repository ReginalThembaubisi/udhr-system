package com.udhr.audit;

import com.udhr.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Map;

/** Record an actual successful CSV download, not just a visit to the reports page. */
@Component
public class ReportExportAuditInterceptor implements HandlerInterceptor {
    private static final Map<String, String> REPORTS = Map.of(
            "/api/prescriptions/report/export", "prescriptions",
            "/api/dispensing/report/export", "dispensing",
            "/api/stock/report/export", "stock transactions",
            "/api/lab-results/report/export", "laboratory results",
            "/api/referrals/report/export", "referrals"
    );
    private static final Map<String, String> REPORT_VIEWS = Map.of(
            "/api/prescriptions/report", "prescriptions",
            "/api/dispensing/report", "dispensing",
            "/api/stock/report", "stock transactions",
            "/api/lab-results/report", "laboratory results",
            "/api/referrals/report", "referrals"
    );
    private final AuditService auditService;

    public ReportExportAuditInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        if (!"GET".equals(request.getMethod()) || response.getStatus() != 200 || ex != null) return;
        String report = REPORTS.get(request.getRequestURI());
        boolean exported = report != null;
        if (!exported) report = REPORT_VIEWS.get(request.getRequestURI());
        if (report == null) return;
        var actor = auditService.currentStaff();
        if (actor != null) {
            auditService.recordEvent(actor, null, exported ? "REPORT_CSV_EXPORTED" : "REPORT_VIEWED",
                    exported ? "downloaded the " + report + " report as a CSV file"
                            : "reviewed the " + report + " report",
                    "REPORT", report.replace(' ', '_'));
        }
    }
}
