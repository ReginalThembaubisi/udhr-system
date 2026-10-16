package com.udhr.privacy;

import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Outside a demo, the logins published in DataSeeder are switched off at
 * startup (POPIA section 19). Runs against its own in-memory database so the
 * other tests keep their demo accounts.
 */
@SpringBootTest(properties = {
        "udhr.demo-accounts=disabled",
        "spring.datasource.url=jdbc:h2:mem:udhr_demo_off;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class DemoAccountsDisabledTest {

    @Autowired private StaffRepository staffRepository;
    @Autowired private PatientRepository patientRepository;

    @Test
    void publishedDemoLoginsAreSwitchedOff() {
        for (String staffNumber : new String[] {"ADMIN001", "DOC001", "NUR001", "PHARM001", "SYSADMIN001"}) {
            assertThat(staffRepository.findByStaffNumber(staffNumber).orElseThrow().getActive())
                    .as(staffNumber).isFalse();
        }
        assertThat(patientRepository.findByIdNumber("9001015000083").orElseThrow().getPortalPinHash()).isNull();
    }
}
