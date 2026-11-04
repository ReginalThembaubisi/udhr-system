package com.udhr.audit;

import com.udhr.dto.AuditLogEntryResponse;
import com.udhr.model.AuditLog;
import com.udhr.service.AuditNarrative;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AuditNarrativeTest {
    @Test
    void auditScreenUsesMeaningfulTitlesInsteadOfCodes() {
        assertThat(AuditNarrative.title("ADMIN_VIEW_AUDIT_LOG")).isEqualTo("Audit history reviewed");
        assertThat(AuditNarrative.title("RECORD_VITALS")).isEqualTo("Vital signs recorded");
        assertThat(AuditNarrative.title("STOCK_RECEIVED")).isEqualTo("Medication stock received");
        assertThat(AuditNarrative.matchingCodes("audit history")).contains("ADMIN_VIEW_AUDIT_LOG");
    }

    @Test
    void anonymousSignInFailuresAreNotMislabelledAsPatients() {
        AuditLog log = new AuditLog();
        log.setAction("LOGIN_FAILED");
        log.setDescription("attempted to sign in with an invalid staff number");
        AuditLogEntryResponse response = AuditLogEntryResponse.from(log);
        assertThat(response.getActorType()).isEqualTo("ANONYMOUS");
        assertThat(response.getStaffRole()).isEqualTo("ANONYMOUS");
        assertThat(response.getTitle()).isEqualTo("Unsuccessful sign-in");
        assertThat(response.getSummary()).contains("Unidentified visitor");
    }

    @Test
    void sensitiveClinicalValuesAreNotReturnedToSystemAdmin() {
        AuditLog log = new AuditLog();
        log.setAction("ADD_DIAGNOSIS");
        log.setDescription("Recorded a confidential diagnosis");
        log.setNewValue("Confidential notes not for administrators");
        AuditLogEntryResponse response = AuditLogEntryResponse.from(log);
        assertThat(response.getNewValue()).isNull();
        assertThat(response.getSummary()).doesNotContain("Confidential");
    }

    @Test
    void operationalStockChangesRetainUsefulBeforeAndAfterDetails() {
        AuditLog log = new AuditLog();
        log.setAction("STOCK_ADJUSTED");
        log.setPreviousValue("20 tablets");
        log.setNewValue("15 tablets");
        AuditLogEntryResponse response = AuditLogEntryResponse.from(log);
        assertThat(response.getPreviousValue()).isEqualTo("20 tablets");
        assertThat(response.getNewValue()).isEqualTo("15 tablets");
    }
}
